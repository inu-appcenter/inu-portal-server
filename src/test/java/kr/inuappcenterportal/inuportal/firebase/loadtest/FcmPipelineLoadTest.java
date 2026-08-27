package kr.inuappcenterportal.inuportal.firebase.loadtest;

import com.google.api.core.SettableApiFuture;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmSendStatus;
import kr.inuappcenterportal.inuportal.domain.firebase.event.FcmEventListener;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmAsyncExecutor;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmTransactionService;
import kr.inuappcenterportal.inuportal.global.config.AsyncConfig;
import kr.inuappcenterportal.inuportal.global.metric.FcmMetrics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * FCM 발송 파이프라인의 부하 테스트.
 * <p>
 * 실제 디바이스 토큰이나 실제 Firebase 호출은 전혀 쓰지 않는다. {@link FirebaseMessaging}을
 * 지연시간·실패율을 흉내내는 가짜로 교체하고, 실 회원 대신 합성 {@code fcm_token} 행을
 * DB(H2)에 대량으로 넣어 서버 파이프라인 자체(배치 발송, JDBC batch insert, AFTER_COMMIT
 * 이벤트 발행, executor 큐잉/재시도)의 동작과 처리량만 관측한다.
 * <p>
 * 기본 빌드(`./gradlew test`)에는 포함되지 않는다. 필요할 때만 {@code ./gradlew loadTest}로
 * 실행한다 (build.gradle의 loadTest 태스크 참고). 규모를 키우고 싶으면 아래 상수를 직접 수정한다.
 */
@Tag("loadtest")
@DataJpaTest
// @DataJpaTest는 기본적으로 각 테스트를 트랜잭션으로 감싸고 끝나면 롤백한다.
// AFTER_COMMIT 이벤트 리스너(FcmEventListener)는 실제 커밋이 있어야만 발화하므로,
// 그 자동 트랜잭션을 꺼서 fcmService의 @Transactional이 실제로 커밋되게 한다.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({AsyncConfig.class, FcmService.class, FcmTransactionService.class, FcmEventListener.class, FcmMetrics.class})
class FcmPipelineLoadTest {

    private static final Logger log = LoggerFactory.getLogger(FcmPipelineLoadTest.class);

    /** 대량 발송 시나리오의 수신자 수. 필요 시 직접 늘려서 실행한다. */
    private static final int BROADCAST_MEMBER_COUNT = 5_000;
    /** 동시성(버스트) 시나리오에서 동시에 쏘는 알림 건수. */
    private static final int BURST_NOTIFICATION_COUNT = 200;
    private static final int BURST_MEMBERS_PER_NOTIFICATION = 3;

    @TestConfiguration
    static class MetricsConfig {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @MockBean
    private FirebaseMessaging firebaseMessaging;

    // 이 파이프라인 경로(prepareTrackedNotification -> dispatchTrackedNotification)에서는
    // 쓰이지 않지만 FcmService 생성자 의존성이라 빈은 있어야 한다.
    @MockBean
    private FcmAsyncExecutor fcmAsyncExecutor;

    @Autowired
    private FcmService fcmService;

    @Autowired
    private FcmMessageRepository fcmMessageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    @Qualifier("messageExecutor")
    private Executor messageExecutorRaw;

    // 두 테스트가 같은 Spring 컨텍스트(=같은 messageExecutor 싱글턴)를 공유한다.
    // 응답 지연을 흉내내는 스케줄러를 테스트마다 껐다 켜면, 아직 완료되지 않은 이전 테스트의
    // 발송 작업이 콜백을 영영 못 받아 60초 타임아웃으로 미확인 처리되며 다음 테스트까지 오염시킨다.
    // 그래서 클래스 전체에서 하나만 만들어 쓰고, 각 테스트가 끝날 때 messageExecutor를 완전히
    // 비운 뒤 리턴해 테스트 간 간섭을 막는다.
    private static ScheduledExecutorService networkSimulator;

    @BeforeAll
    static void startNetworkSimulator() {
        networkSimulator = Executors.newScheduledThreadPool(8, r -> {
            Thread t = new Thread(r, "firebase-sim-");
            t.setDaemon(true);
            return t;
        });
    }

    @AfterAll
    static void stopNetworkSimulator() {
        networkSimulator.shutdownNow();
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM member_fcm_message");
        jdbcTemplate.update("DELETE FROM fcm_message");
        jdbcTemplate.update("DELETE FROM fcm_token");
    }

    private ThreadPoolTaskExecutor messageExecutor() {
        return (ThreadPoolTaskExecutor) messageExecutorRaw;
    }

    private void awaitMessageExecutorDrain(Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        ThreadPoolTaskExecutor executor = messageExecutor();
        while (System.nanoTime() < deadline) {
            if (executor.getActiveCount() == 0 && executor.getThreadPoolExecutor().getQueue().isEmpty()) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("messageExecutor가 " + timeout.getSeconds()
                + "초 안에 비지 않았다 (다음 테스트로 새는 비동기 작업 의심)");
    }

    @Test
    void 대량_수신자_발송시_파이프라인_처리량을_측정한다() throws Exception {
        int memberCount = BROADCAST_MEMBER_COUNT;
        insertSyntheticTokens(memberCount);
        stubRealisticFirebase(Duration.ofMillis(150), 0.02); // 배치당 150ms, 토큰당 2% 실패율(재시도 가능)

        List<Long> memberIds = LongStream.rangeClosed(1, memberCount).boxed().toList();

        long dbWriteStartNanos = System.nanoTime();
        var dispatch = fcmService.prepareTrackedNotification(
                memberIds, "부하테스트", "대량 발송 처리량 측정", FcmMessageType.GENERAL, null, "/loadtest");
        long dbWriteElapsedMs = Duration.ofNanos(System.nanoTime() - dbWriteStartNanos).toMillis();

        assertThat(dispatch).isNotNull();
        Long fcmMessageId = dispatch.fcmMessageId();

        long dispatchStartNanos = System.nanoTime();
        FcmMessage finalState = pollUntilTerminal(fcmMessageId, Duration.ofSeconds(60));
        long dispatchElapsedMs = Duration.ofNanos(System.nanoTime() - dispatchStartNanos).toMillis();

        Integer storedRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM member_fcm_message WHERE fcm_message_id = ?", Integer.class, fcmMessageId);
        int expectedBatches = (int) Math.ceil(memberCount / 500.0);

        log.info("""
                        ===== FCM 파이프라인 부하 테스트: 대량 발송 =====
                        대상 인원            : {}
                        예상 배치 수(500/배치) : {}
                        DB 저장(동기, 호출 스레드 점유) : {} ms
                        AFTER_COMMIT -> 발송 완료까지  : {} ms
                        최종 상태             : {} (성공 {} / 실패 {} / 대상 {})
                        member_fcm_message 저장 행 수 : {} (기대값 {})
                        처리량               : {} 건/초
                        =================================================""",
                memberCount, expectedBatches, dbWriteElapsedMs, dispatchElapsedMs,
                finalState.getSendStatus(), finalState.getSendCount(), finalState.getFailureCount(), finalState.getTargetCount(),
                storedRows, memberCount,
                String.format("%.1f", memberCount / Math.max(dispatchElapsedMs / 1000.0, 0.001)));

        // 데이터 무결성: 유니크 제약(fcm_message_id, member_id)에 막혀 유실된 행이 없어야 한다.
        assertThat(storedRows).isEqualTo(memberCount);
        // 대상이 있었는데 PENDING/PROCESSING에 멈춰있으면 이벤트 유실이나 교착을 의심해야 한다.
        assertThat(finalState.getSendStatus()).isIn(FcmSendStatus.SUCCESS, FcmSendStatus.PARTIAL_FAILURE);
        assertThat(finalState.getSendCount() + finalState.getFailureCount()).isEqualTo(memberCount);

        awaitMessageExecutorDrain(Duration.ofSeconds(10));
    }

    @Test
    void 동시_알림_버스트시_messageExecutor_포화_동작을_확인한다() throws Exception {
        int notificationCount = BURST_NOTIFICATION_COUNT;
        int membersPerNotification = BURST_MEMBERS_PER_NOTIFICATION;
        insertSyntheticTokens(notificationCount * membersPerNotification);

        List<String> executionThreadNames = new CopyOnWriteArrayList<>();
        trackDispatchThreadNames(executionThreadNames);

        ExecutorService submitters = Executors.newFixedThreadPool(50, r -> {
            Thread t = new Thread(r, "burst-submitter-");
            t.setDaemon(true);
            return t;
        });
        CountDownLatch ready = new CountDownLatch(notificationCount);
        AtomicInteger nextMemberId = new AtomicInteger(1);
        List<Long> fcmMessageIds = new CopyOnWriteArrayList<>();

        for (int i = 0; i < notificationCount; i++) {
            submitters.submit(() -> {
                try {
                    int start = nextMemberId.getAndAdd(membersPerNotification);
                    List<Long> memberIds = LongStream.range(start, start + membersPerNotification).boxed().toList();
                    var dispatch = fcmService.prepareTrackedNotification(
                            memberIds, "버스트", "동시 발송 테스트", FcmMessageType.POST_REPLY, null, "/loadtest/burst");
                    if (dispatch != null) {
                        fcmMessageIds.add(dispatch.fcmMessageId());
                    }
                } finally {
                    ready.countDown();
                }
            });
        }

        assertThat(ready.await(30, TimeUnit.SECONDS)).as("모든 알림 저장 트랜잭션 제출").isTrue();
        submitters.shutdown();

        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        int doneCount = 0;
        while (System.nanoTime() < deadline) {
            doneCount = (int) fcmMessageIds.stream()
                    .map(id -> fcmMessageRepository.findById(id).orElseThrow())
                    .filter(m -> isTerminal(m.getSendStatus()))
                    .count();
            if (doneCount == fcmMessageIds.size()) break;
            Thread.sleep(100);
        }

        long poolThreadRuns = executionThreadNames.stream().filter(n -> n.startsWith("FCM-MSG-")).count();
        long callerRunsFallback = executionThreadNames.size() - poolThreadRuns;
        Set<String> distinctPoolThreads = executionThreadNames.stream()
                .filter(n -> n.startsWith("FCM-MSG-"))
                .collect(java.util.stream.Collectors.toSet());

        log.info("""
                        ===== FCM 파이프라인 부하 테스트: 동시 버스트 =====
                        동시 발행 알림 건수        : {}
                        완료 확인 건수            : {} / {}
                        messageExecutor 풀 스레드로 처리 : {} 건 (서로 다른 스레드 {}개, core=2/max=4)
                        CallerRunsPolicy로 호출 스레드에서 처리 : {} 건 (풀+큐 용량(2~54) 초과분)
                        =====================================================""",
                notificationCount, doneCount, fcmMessageIds.size(),
                poolThreadRuns, distinctPoolThreads.size(), callerRunsFallback);

        // CallerRunsPolicy 덕분에 큐잉 용량을 넘겨도 유실 없이 전부 처리되어야 한다.
        assertThat(doneCount).isEqualTo(fcmMessageIds.size());
        assertThat(fcmMessageIds).hasSize(notificationCount);

        awaitMessageExecutorDrain(Duration.ofSeconds(10));
    }

    private boolean isTerminal(FcmSendStatus status) {
        return status == FcmSendStatus.SUCCESS || status == FcmSendStatus.PARTIAL_FAILURE
                || status == FcmSendStatus.FAILED || status == FcmSendStatus.NO_TARGET;
    }

    private FcmMessage pollUntilTerminal(Long fcmMessageId, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            FcmMessage message = fcmMessageRepository.findById(fcmMessageId).orElseThrow();
            if (isTerminal(message.getSendStatus())) {
                return message;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("fcmMessageId=" + fcmMessageId + "가 " + timeout.getSeconds()
                + "초 안에 종결 상태에 도달하지 못했다 (이벤트 유실/교착 의심)");
    }

    /** member 테이블 없이, FK 제약이 없는 fcm_token.member_id에 합성 ID를 바로 채워 넣는다. */
    private void insertSyntheticTokens(int count) {
        LocalDateTime now = LocalDateTime.now();
        List<Long> memberIds = LongStream.rangeClosed(1, count).boxed().toList();
        jdbcTemplate.batchUpdate(
                "INSERT INTO fcm_token (member_id, token, device_type, create_date) VALUES (?, ?, ?, ?)",
                memberIds, 500,
                (PreparedStatement ps, Long memberId) -> {
                    ps.setLong(1, memberId);
                    ps.setString(2, "loadtest-token-" + memberId);
                    ps.setString(3, "android");
                    ps.setObject(4, now);
                });
    }

    /**
     * FirebaseMessaging.sendEachForMulticastAsync를 실제 네트워크 호출 없이,
     * 지정한 지연시간 뒤에 지정한 실패율로 결과를 완료시키는 가짜로 대체한다.
     */
    private void stubRealisticFirebase(Duration latency, double failureRate) {
        when(firebaseMessaging.sendEachForMulticastAsync(any(MulticastMessage.class)))
                .thenAnswer(invocation -> {
                    MulticastMessage message = invocation.getArgument(0);
                    @SuppressWarnings("unchecked")
                    List<String> tokens = (List<String>) org.springframework.test.util.ReflectionTestUtils
                            .getField(message, "tokens");
                    SettableApiFuture<BatchResponse> future = SettableApiFuture.create();
                    networkSimulator.schedule(() -> {
                        try {
                            List<SendResponse> responses = new ArrayList<>();
                            for (int i = 0; i < tokens.size(); i++) {
                                boolean fail = ThreadLocalRandom.current().nextDouble() < failureRate;
                                responses.add(fail ? failure(MessagingErrorCode.UNAVAILABLE) : success());
                            }
                            future.set(batchResponse(responses));
                        } catch (Throwable t) {
                            // ScheduledExecutorService는 실행 중 예외를 조용히 삼킨다. future가 영영 완료되지
                            // 않아 60초 뒤 타임아웃으로만 보이면 원인 추적이 안 되므로 여기서 직접 실패시킨다.
                            future.setException(t);
                        }
                    }, latency.toMillis(), TimeUnit.MILLISECONDS);
                    return future;
                });
    }

    /** dispatchToMembersInternal이 실제로 어느 스레드에서 도는지(풀 스레드 vs CallerRunsPolicy 전락) 기록한다. */
    private void trackDispatchThreadNames(List<String> sink) {
        when(firebaseMessaging.sendEachForMulticastAsync(any(MulticastMessage.class)))
                .thenAnswer(invocation -> {
                    sink.add(Thread.currentThread().getName());
                    MulticastMessage message = invocation.getArgument(0);
                    @SuppressWarnings("unchecked")
                    List<String> tokens = (List<String>) org.springframework.test.util.ReflectionTestUtils
                            .getField(message, "tokens");
                    List<SendResponse> responses = new ArrayList<>();
                    for (int i = 0; i < tokens.size(); i++) {
                        responses.add(success());
                    }
                    SettableApiFuture<BatchResponse> settable = SettableApiFuture.create();
                    networkSimulator.schedule(() -> {
                        try {
                            settable.set(batchResponse(responses));
                        } catch (Throwable t) {
                            settable.setException(t);
                        }
                    }, 50, TimeUnit.MILLISECONDS);
                    return settable;
                });
    }

    private BatchResponse batchResponse(List<SendResponse> responses) {
        // 다른 mock(SendResponse)의 스텁된 메서드(isSuccessful)를, response의 when()이 아직
        // 안 끝난 상태에서 호출하면 Mockito가 UnfinishedStubbingException을 던진다.
        // (이 세션에서 FcmRetryTest 작성 때도 겪었던 것과 같은 문제) when() 시작 전에 값부터 계산한다.
        int successCount = (int) responses.stream().filter(SendResponse::isSuccessful).count();
        int failureCount = responses.size() - successCount;

        BatchResponse response = mock(BatchResponse.class);
        when(response.getResponses()).thenReturn(Collections.unmodifiableList(responses));
        when(response.getSuccessCount()).thenReturn(successCount);
        when(response.getFailureCount()).thenReturn(failureCount);
        return response;
    }

    private SendResponse success() {
        SendResponse response = mock(SendResponse.class);
        when(response.isSuccessful()).thenReturn(true);
        return response;
    }

    private SendResponse failure(MessagingErrorCode errorCode) {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(errorCode);

        SendResponse response = mock(SendResponse.class);
        when(response.isSuccessful()).thenReturn(false);
        when(response.getException()).thenReturn(exception);
        return response;
    }
}
