package kr.inuappcenterportal.inuportal.domain.firebase.service;

import com.google.api.core.ApiFutures;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.AdminNotificationDispatch;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageFailedTargetRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.global.metric.FcmMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    private static final int CHUNK_SIZE = 100;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private FcmMessageRepository fcmMessageRepository;

    @Mock
    private FcmMetrics fcmMetrics;

    @Mock
    private FcmTransactionService fcmTransactionService;

    @Mock
    private FcmAsyncExecutor fcmAsyncExecutor;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private FcmDispatchGate fcmDispatchGate;

    @Mock
    private FcmFailedTargetService fcmFailedTargetService;

    @Mock
    private FcmMessageFailedTargetRepository fcmMessageFailedTargetRepository;

    @InjectMocks
    private FcmService fcmService;

    /** 게이트가 실제로 잘라낸 청크. 서비스가 청크당 정확히 한 번씩 발송했는지 대조하는 용도. */
    private final List<List<String>> issuedChunks = new ArrayList<>();

    /**
     * 게이트는 목이지만 동작은 실물에 위임한다. 그래야 "서비스가 게이트를 거쳐 청크 단위로 보낸다"는
     * 경로 전체가 검증되고, 동시성 상한 자체는 {@link FcmDispatchGateTest}가 따로 책임진다.
     */
    @BeforeEach
    void delegateGateToRealImplementation() throws Exception {
        FcmDispatchGate real = new FcmDispatchGate(firebaseMessaging, CHUNK_SIZE, 2, 10_000L);
        issuedChunks.clear();

        // isRetryable/backoff 테스트는 게이트를 쓰지 않으므로 이 스텁만 lenient로 둔다.
        // 클래스 전체를 LENIENT로 열면 진짜 불필요한 스텁까지 묻힌다.
        lenient().when(fcmDispatchGate.chunk(anyList())).thenAnswer(invocation -> {
            List<List<String>> chunks = real.chunk(invocation.getArgument(0));
            issuedChunks.addAll(chunks);
            return chunks;
        });
        lenient().when(fcmDispatchGate.sendAwait(any(MulticastMessage.class), anyLong()))
                .thenAnswer(invocation -> real.sendAwait(invocation.getArgument(0), invocation.getArgument(1)));
    }

    @Test
    @DisplayName("토큰 250개는 100개 단위 청크 3개로 나뉘고, 청크마다 정확히 한 번씩 발송된다")
    void sendToMembers_dispatchesOncePerChunk() {
        Map<String, Long> tokenAndMemberId = new LinkedHashMap<>();
        for (int i = 1; i <= 250; i++) {
            tokenAndMemberId.put("token_" + i, (long) i);
        }
        AdminNotificationDispatch dispatch = new AdminNotificationDispatch(
                1L, "Title", "Body", tokenAndMemberId, List.of(), null);

        // successBatch가 내부에서 스터빙하므로 when(...) 인자 안에서 만들면 중첩 스터빙이 된다.
        BatchResponse first = successBatch(100);
        BatchResponse second = successBatch(100);
        BatchResponse third = successBatch(50);
        when(firebaseMessaging.sendEachForMulticastAsync(any(MulticastMessage.class)))
                .thenReturn(ApiFutures.immediateFuture(first))
                .thenReturn(ApiFutures.immediateFuture(second))
                .thenReturn(ApiFutures.immediateFuture(third));

        fcmService.sendToMembers(dispatch);

        assertThat(issuedChunks)
                .as("청크 크기가 곧 동시 커넥션 수이므로 100을 넘으면 안 된다")
                .extracting(List::size)
                .containsExactly(100, 100, 50);
        verify(firebaseMessaging, times(3)).sendEachForMulticastAsync(any(MulticastMessage.class));
        verify(fcmTransactionService).updateFinalStatus(1L, 250, 0);
    }

    @Test
    @DisplayName("전송 대상이 청크 하나에 들어가면 발송도 한 번만 일어난다")
    void sendToMembers_singleChunkDispatchesOnce() {
        Map<String, Long> tokenAndMemberId = new LinkedHashMap<>();
        for (int i = 1; i <= 40; i++) {
            tokenAndMemberId.put("token_" + i, (long) i);
        }
        AdminNotificationDispatch dispatch = new AdminNotificationDispatch(
                2L, "Title", "Body", tokenAndMemberId, List.of(), null);

        BatchResponse only = successBatch(40);
        when(firebaseMessaging.sendEachForMulticastAsync(any(MulticastMessage.class)))
                .thenReturn(ApiFutures.immediateFuture(only));

        fcmService.sendToMembers(dispatch);

        assertThat(issuedChunks).extracting(List::size).containsExactly(40);
        verify(firebaseMessaging, times(1)).sendEachForMulticastAsync(any(MulticastMessage.class));
        verify(fcmTransactionService).updateFinalStatus(2L, 40, 0);
    }

    /**
     * 재시도가 "실패한 사람에게만" 나가려면 이 기록이 정확해야 한다. 특히 한 회원이 기기를
     * 여러 대 쓸 때, 하나라도 전달됐으면 그 회원은 이미 알림을 받은 것이므로 실패자로 남으면 안 된다.
     * 남기면 다음 재시도에서 같은 알림을 두 번 받는다.
     */
    @Test
    @DisplayName("토큰 하나라도 전달된 회원은 실패 기록에서 빠지고, 전부 실패한 회원만 남는다")
    void sendToMembers_recordsOnlyMembersWithNoSuccessfulToken() {
        Map<String, Long> tokenAndMemberId = new LinkedHashMap<>();
        tokenAndMemberId.put("memberA_phone", 1L);   // 성공
        tokenAndMemberId.put("memberA_tablet", 1L);  // 실패 → 그래도 A는 받았으므로 제외
        tokenAndMemberId.put("memberB_phone", 2L);   // 실패 → B는 전부 실패
        tokenAndMemberId.put("orphan_token", -1L);   // 회원 없는 토큰 → 대상 특정 불가라 제외

        AdminNotificationDispatch dispatch = new AdminNotificationDispatch(
                7L, "Title", "Body", tokenAndMemberId, List.of(), null);

        BatchResponse batch = mixedBatch(List.of(true, false, false, false));
        when(firebaseMessaging.sendEachForMulticastAsync(any(MulticastMessage.class)))
                .thenReturn(ApiFutures.immediateFuture(batch));

        fcmService.sendToMembers(dispatch);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Long>> captor = ArgumentCaptor.forClass(Set.class);
        verify(fcmFailedTargetService).replaceFailedTargets(eq(7L), captor.capture());
        assertThat(captor.getValue()).containsExactly(2L);
    }

    /**
     * 2026-09-07 장애 로그 6,648건 전수 분류 결과를 그대로 고정한다.
     * 무효 토큰은 항상 MessagingErrorCode가 채워져 오고, errorCode가 비어 있는 건은 전부 타임아웃이었다.
     */
    @Test
    @DisplayName("무효 토큰은 errorCode로 판별해 재시도하지 않고, errorCode가 없는 타임아웃은 재시도한다")
    void isRetryable_followsObservedErrorCodes() throws Exception {
        assertThat(isRetryable(exception(MessagingErrorCode.UNREGISTERED, "NotRegistered"))).isFalse();
        assertThat(isRetryable(exception(MessagingErrorCode.UNREGISTERED, "APNs device token is disabled."))).isFalse();
        assertThat(isRetryable(exception(MessagingErrorCode.INVALID_ARGUMENT, "Invalid registration token"))).isFalse();

        assertThat(isRetryable(exception(null, "Timed out while making an API call: Connect timed out"))).isTrue();
        assertThat(isRetryable(exception(null, "Timed out while making an API call: Read timed out"))).isTrue();
        assertThat(isRetryable(exception(MessagingErrorCode.INTERNAL, "Internal error occurred."))).isTrue();
        assertThat(isRetryable(exception(MessagingErrorCode.UNAVAILABLE, "Service unavailable"))).isTrue();
        assertThat(isRetryable(exception(MessagingErrorCode.QUOTA_EXCEEDED, "Quota exceeded"))).isTrue();
    }

    /**
     * 사유를 알 수 없는 실패를 영구 실패로 단정하면 알림이 조용히 유실된다.
     * 메시지 문자열에 "disabled"/"invalid" 같은 단어가 섞였다는 이유만으로 재시도를 포기해선 안 된다.
     */
    @Test
    @DisplayName("errorCode가 없으면 메시지에 disabled/invalid가 섞여 있어도 재시도한다")
    void isRetryable_doesNotGuessFromMessageText() throws Exception {
        assertThat(isRetryable(exception(null, "Connection reset: TLSv1 is disabled"))).isTrue();
        assertThat(isRetryable(exception(null, "invalid gateway response, retry later"))).isTrue();
        assertThat(isRetryable(exception(null, null))).isTrue();
        assertThat(isRetryable(null)).isTrue();
    }

    @Test
    @DisplayName("재시도 대기는 지수적으로 늘고 지터가 붙어 재시도 시점이 흩어진다")
    void backoffMillis_isExponentialWithJitter() throws Exception {
        Method method = FcmService.class.getDeclaredMethod("backoffMillis", int.class);
        method.setAccessible(true);

        for (int attempt = 1; attempt <= 4; attempt++) {
            long base = 1000L * (1L << (attempt - 1));
            for (int i = 0; i < 50; i++) {
                long actual = (long) method.invoke(fcmService, attempt);
                assertThat(actual).isBetween(base, base + 499);
            }
        }
    }

    private boolean isRetryable(FirebaseMessagingException exception) throws Exception {
        Method method = FcmService.class.getDeclaredMethod("isRetryable", FirebaseMessagingException.class);
        method.setAccessible(true);
        return (boolean) method.invoke(fcmService, exception);
    }

    /**
     * {@code logMessage}는 장애 로그에 실제로 찍혔던 문구다. isRetryable이 메시지 문자열을
     * 보지 않는다는 것이 이 테스트의 요지이므로 일부러 스텁하지 않고 문서 용도로만 남긴다.
     */
    private FirebaseMessagingException exception(MessagingErrorCode code, String logMessage) {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(code);
        return exception;
    }

    /** 성공/실패가 섞인 배치. 마지막 시도까지 실패한 토큰은 영구 실패로 확정된다. */
    private BatchResponse mixedBatch(List<Boolean> outcomes) {
        BatchResponse batchResponse = mock(BatchResponse.class);
        FirebaseMessagingException permanent = mock(FirebaseMessagingException.class);
        when(permanent.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);

        List<SendResponse> responses = new ArrayList<>();
        for (Boolean successful : outcomes) {
            SendResponse response = mock(SendResponse.class);
            when(response.isSuccessful()).thenReturn(successful);
            if (!successful) {
                when(response.getException()).thenReturn(permanent);
            }
            responses.add(response);
        }
        when(batchResponse.getResponses()).thenReturn(responses);
        return batchResponse;
    }

    private BatchResponse successBatch(int successCount) {
        BatchResponse batchResponse = mock(BatchResponse.class);
        List<SendResponse> responses = new ArrayList<>();
        for (int i = 0; i < successCount; i++) {
            SendResponse response = mock(SendResponse.class);
            when(response.isSuccessful()).thenReturn(true);
            responses.add(response);
        }
        when(batchResponse.getResponses()).thenReturn(responses);
        return batchResponse;
    }
}
