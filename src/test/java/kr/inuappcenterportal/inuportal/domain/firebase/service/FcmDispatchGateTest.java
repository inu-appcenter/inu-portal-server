package kr.inuappcenterportal.inuportal.domain.firebase.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FcmDispatchGateTest {

    private static MulticastMessage anyMessage() {
        return MulticastMessage.builder().addToken("token").build();
    }

    @Test
    @DisplayName("토큰 250개는 청크 크기 100 기준으로 100/100/50 세 덩어리로 나뉜다")
    void chunk_splitsByConfiguredChunkSize() {
        FcmDispatchGate gate = new FcmDispatchGate(mock(FirebaseMessaging.class), 100, 2, 1_000L);

        List<String> tokens = IntStream.range(0, 250).mapToObj(i -> "token_" + i).toList();

        List<List<String>> chunks = gate.chunk(tokens);

        assertThat(chunks).hasSize(3);
        assertThat(chunks).extracting(List::size).containsExactly(100, 100, 50);
        assertThat(chunks.stream().flatMap(List::stream)).containsExactlyElementsOf(tokens);
    }

    @Test
    @DisplayName("빈 토큰 목록은 청크를 만들지 않는다")
    void chunk_emptyInputProducesNoChunks() {
        FcmDispatchGate gate = new FcmDispatchGate(mock(FirebaseMessaging.class), 100, 2, 1_000L);

        assertThat(gate.chunk(List.of())).isEmpty();
    }

    /**
     * 이 테스트가 이번 수정의 핵심이다.
     *
     * <p>firebase-admin은 청크 하나를 청크 크기만큼의 동시 HTTP 요청으로 팬아웃하므로,
     * 실제 동시 커넥션 수는 {@code 청크 크기 × 동시 실행 청크 수}가 된다. 여러 발송 경로가
     * 동시에 밀고 들어와도 후자가 설정값을 넘지 않아야 한다.
     */
    @Test
    @DisplayName("여러 스레드가 동시에 밀어넣어도 동시 실행 청크 수가 상한을 넘지 않는다")
    void send_neverExceedsConfiguredConcurrency() throws Exception {
        int maxConcurrentChunks = 2;
        int callers = 16;

        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger peakInFlight = new AtomicInteger();

        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenAnswer(invocation -> {
            int current = inFlight.incrementAndGet();
            peakInFlight.accumulateAndGet(current, Math::max);
            try {
                Thread.sleep(30L);
            } finally {
                inFlight.decrementAndGet();
            }
            return mock(BatchResponse.class);
        });

        FcmDispatchGate gate = new FcmDispatchGate(messaging, 100, maxConcurrentChunks, 10_000L);

        ExecutorService pool = Executors.newFixedThreadPool(callers);
        CountDownLatch startTogether = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(callers);
        List<Throwable> failures = new ArrayList<>();

        try {
            for (int i = 0; i < callers; i++) {
                pool.submit(() -> {
                    try {
                        startTogether.await();
                        gate.send(anyMessage());
                    } catch (Throwable t) {
                        synchronized (failures) {
                            failures.add(t);
                        }
                    } finally {
                        finished.countDown();
                    }
                });
            }
            startTogether.countDown();
            assertThat(finished.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(failures).isEmpty();
        assertThat(peakInFlight.get())
                .as("동시 실행 청크 수가 상한을 넘으면 장애 당시의 커넥션 고갈이 재현된다")
                .isLessThanOrEqualTo(maxConcurrentChunks);
        assertThat(peakInFlight.get())
                .as("상한까지는 실제로 병렬로 돌아야 한다")
                .isEqualTo(maxConcurrentChunks);
    }

    @Test
    @DisplayName("허가를 얻지 못하면 발송을 시도하지 않고 TimeoutException을 던진다")
    void send_throwsTimeoutWhenPermitUnavailable() throws Exception {
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        CountDownLatch holderInside = new CountDownLatch(1);
        CountDownLatch releaseHolder = new CountDownLatch(1);

        when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenAnswer(invocation -> {
            holderInside.countDown();
            releaseHolder.await(10, TimeUnit.SECONDS);
            return mock(BatchResponse.class);
        });

        // 허가 1개짜리 게이트를 한 스레드가 점유한 상태에서 다른 호출이 들어오는 상황
        FcmDispatchGate gate = new FcmDispatchGate(messaging, 100, 1, 100L);

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            pool.submit(() -> gate.send(anyMessage()));
            assertThat(holderInside.await(10, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> gate.send(anyMessage()))
                    .isInstanceOf(TimeoutException.class);
        } finally {
            releaseHolder.countDown();
            pool.shutdownNow();
        }
    }
}
