package kr.inuappcenterportal.inuportal.firebase;

import com.google.api.core.ApiFutures;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import kr.inuappcenterportal.inuportal.config.FcmTestAsyncConfig;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.TrackedNotificationDispatch;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmAsyncExecutor;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmTransactionService;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.global.metric.FcmMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {FcmTestAsyncConfig.class, FcmService.class, FcmTransactionService.class})
class FcmRetryTest {

    @MockBean
    private FcmTokenRepository fcmTokenRepository;

    @MockBean
    private FcmMessageRepository fcmMessageRepository;

    @MockBean
    private MemberFcmMessageRepository memberFcmMessageRepository;

    @MockBean
    private FirebaseMessaging firebaseMessaging;

    @MockBean
    private MemberRepository memberRepository;

    @MockBean
    private SemesterRepository semesterRepository;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private FcmTransactionService fcmTransactionService;

    @MockBean
    private FcmAsyncExecutor fcmAsyncExecutor;

    @MockBean
    private FcmMetrics fcmMetrics;

    @Autowired
    private FcmService fcmService;

    @Test
    @DisplayName("재시도 시 실패한 토큰만 재발송하고 성공한 토큰은 다시 보내지 않는다")
    void retriesOnlyFailedTokens() {
        // 1차: a 성공, b 실패(UNAVAILABLE) / 2차: b 성공
        BatchResponse first = batchResponse(success(), failure(MessagingErrorCode.UNAVAILABLE));
        BatchResponse second = batchResponse(success());

        when(firebaseMessaging.sendEachForMulticastAsync(any(MulticastMessage.class)))
                .thenReturn(ApiFutures.immediateFuture(first))
                .thenReturn(ApiFutures.immediateFuture(second));

        fcmService.dispatchTrackedNotification(dispatch("token_a", "token_b"));

        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(firebaseMessaging, times(2)).sendEachForMulticastAsync(captor.capture());

        assertThat(tokensOf(captor.getAllValues().get(0))).containsExactly("token_a", "token_b");
        // 재시도에는 실패한 token_b만 담겨야 한다. token_a가 다시 들어가면 중복 푸시가 된다.
        assertThat(tokensOf(captor.getAllValues().get(1))).containsExactly("token_b");

        verify(fcmTransactionService).updateFinalStatus(1L, 2, 0);
    }

    @Test
    @DisplayName("토큰이 무효한 실패(UNREGISTERED)는 재시도하지 않고 즉시 실패로 확정한다")
    void doesNotRetryPermanentFailure() {
        BatchResponse response = batchResponse(success(), failure(MessagingErrorCode.UNREGISTERED));

        when(firebaseMessaging.sendEachForMulticastAsync(any(MulticastMessage.class)))
                .thenReturn(ApiFutures.immediateFuture(response));

        fcmService.dispatchTrackedNotification(dispatch("token_a", "token_b"));

        verify(firebaseMessaging, times(1)).sendEachForMulticastAsync(any(MulticastMessage.class));
        verify(fcmTransactionService).updateFinalStatus(1L, 1, 1);
    }

    @Test
    @DisplayName("호출이 실패해 결과를 모르면 재발송하지 않고 미확인으로 남긴다")
    void doesNotResendWhenOutcomeIsUnknown() {
        when(firebaseMessaging.sendEachForMulticastAsync(any(MulticastMessage.class)))
                .thenReturn(ApiFutures.immediateFailedFuture(new IllegalStateException("timeout")));

        fcmService.dispatchTrackedNotification(dispatch("token_a", "token_b"));

        // 이미 나간 요청이 성공했을 수 있으므로 재발송하지 않는다.
        verify(firebaseMessaging, times(1)).sendEachForMulticastAsync(any(MulticastMessage.class));
        // 미확인 건을 실패로 단정하지 않는다.
        verify(fcmTransactionService).updateFinalStatus(1L, 0, 0);
        verify(fcmMetrics).recordBatch(eq("POST_REPLY"), eq(2), eq(0), eq(0), eq(2), anyLong());
    }

    private TrackedNotificationDispatch dispatch(String... tokens) {
        Map<String, Long> tokenAndMemberId = new LinkedHashMap<>();
        long memberId = 69L;
        for (String token : tokens) {
            tokenAndMemberId.put(token, memberId++);
        }
        return new TrackedNotificationDispatch(
                1L, tokenAndMemberId, "Test Title", "Test Content",
                FcmMessageType.POST_REPLY, 55L, "/posts/55");
    }

    private BatchResponse batchResponse(SendResponse... responses) {
        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getResponses()).thenReturn(List.of(responses));
        return batchResponse;
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

    @SuppressWarnings("unchecked")
    private List<String> tokensOf(MulticastMessage message) {
        return (List<String>) ReflectionTestUtils.getField(message, "tokens");
    }
}
