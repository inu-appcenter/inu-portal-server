package kr.inuappcenterportal.inuportal.domain.firebase.service;

import com.google.api.core.ApiFuture;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * FCM 발송 동시성 게이트.
 *
 * <p><b>왜 필요한가.</b> firebase-admin 9.2.0의 {@code sendEachForMulticast}는 이름과 달리 배치
 * API가 아니다. 내부적으로 토큰 1건당 HTTP 요청 1건을 만들어 {@code Executors.newCachedThreadPool()}
 * (상한 없음)에 전부 던지고 완료를 기다린다. 따라서 <b>청크 하나를 보내는 것만으로 청크 크기만큼의
 * TCP 커넥션이 동시에 열린다.</b>
 *
 * <p>2026-09-07 관리자 발송 장애(fcmMessageId=13030)가 정확히 이 구조 때문에 발생했다. 대상 3,296건을
 * 500개 청크로 보냈고, 청크 500개가 곧 동시 커넥션 500개가 되어 커넥션 수립 자체가 5초
 * ({@code connectTimeout}) 안에 끝나지 않았다. 로그상 한 청크의 500건이 <b>모두 같은 초에</b> 타임아웃
 * 났는데, 이는 큐에서 순차 대기하다 만료된 것이 아니라 500개가 동시에 출발해 동시에 죽었다는 뜻이다.
 * 결과적으로 청크 7개 중 4개가 3회 재시도까지 전멸해 성공률이 34.9%에 그쳤다.
 *
 * <p><b>왜 청크 크기 축소만으로는 부족한가.</b> 실제 동시 커넥션 수는
 * {@code 청크 크기 × 동시 실행 청크 수}다. {@code sendToAll}은 청크를 {@code sendExecutor}
 * (코어 8 / 최대 16)에 비동기로 흩뿌리므로, 청크를 100으로 줄여도 8~16개가 동시에 각각 100개씩
 * 팬아웃해 최대 1,600 커넥션이 된다. 장애가 났던 단일 스레드 경로(500)보다 오히려 나쁘다.
 * 그래서 청크 크기와 동시 실행 청크 수를 <b>양쪽 모두</b> 묶어야 하고, 후자는 발송 경로가 여럿이므로
 * 프로세스 전역에서 공유하는 세마포어여야 한다.
 *
 * <p>모든 팬아웃 호출은 이 게이트를 거친다. {@code FirebaseMessaging}을 직접 호출하면 상한이 깨진다.
 */
@Slf4j
@Component
public class FcmDispatchGate {

    private final FirebaseMessaging firebaseMessaging;
    private final Semaphore permits;
    private final int chunkSize;
    private final long acquireTimeoutMillis;

    public FcmDispatchGate(
            FirebaseMessaging firebaseMessaging,
            @Value("${fcm.dispatch.chunk-size:100}") int chunkSize,
            @Value("${fcm.dispatch.max-concurrent-chunks:2}") int maxConcurrentChunks,
            @Value("${fcm.dispatch.acquire-timeout-millis:120000}") long acquireTimeoutMillis) {
        this.firebaseMessaging = firebaseMessaging;
        this.chunkSize = chunkSize;
        this.permits = new Semaphore(maxConcurrentChunks);
        this.acquireTimeoutMillis = acquireTimeoutMillis;
        log.info("FCM dispatch gate: chunkSize={}, maxConcurrentChunks={}, maxConcurrentConnections={}",
                chunkSize, maxConcurrentChunks, chunkSize * maxConcurrentChunks);
    }

    /**
     * 토큰 목록을 청크 크기 단위로 자른다. 청크 크기가 곧 동시 커넥션 수이므로
     * 발송 경로마다 제각기 상수를 두지 않고 여기 한 곳에서만 정한다.
     */
    public List<List<String>> chunk(List<String> tokens) {
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i += chunkSize) {
            chunks.add(tokens.subList(i, Math.min(i + chunkSize, tokens.size())));
        }
        return chunks;
    }

    public int chunkSize() {
        return chunkSize;
    }

    /**
     * 허가를 받은 뒤 동기 발송한다.
     *
     * @throws TimeoutException 허가 대기가 {@code acquireTimeoutMillis}를 넘긴 경우.
     *                          발송을 시도조차 하지 않았으므로 호출자는 미발송으로 집계해야 한다.
     */
    public BatchResponse send(MulticastMessage message)
            throws FirebaseMessagingException, InterruptedException, TimeoutException {
        acquire();
        try {
            return firebaseMessaging.sendEachForMulticast(message);
        } finally {
            permits.release();
        }
    }

    /**
     * 허가를 받은 뒤 비동기로 발송하고 {@code timeoutMillis}까지 기다린다.
     *
     * <p>대기가 만료되면 future를 취소하되, <b>이미 나간 요청이 있을 수 있으므로 결과는 미확인</b>이다.
     * 호출자는 이를 실패로 확정해 재전송하면 중복 푸시가 나갈 수 있다.
     */
    public BatchResponse sendAwait(MulticastMessage message, long timeoutMillis) throws Exception {
        acquire();
        ApiFuture<BatchResponse> future = null;
        try {
            future = firebaseMessaging.sendEachForMulticastAsync(message);
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
            throw e;
        } finally {
            permits.release();
        }
    }

    private void acquire() throws InterruptedException, TimeoutException {
        if (!permits.tryAcquire(acquireTimeoutMillis, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException(
                    "FCM 발송 허가 대기 시간 초과 (" + acquireTimeoutMillis + "ms). 동시 발송이 밀려 있습니다.");
        }
    }
}
