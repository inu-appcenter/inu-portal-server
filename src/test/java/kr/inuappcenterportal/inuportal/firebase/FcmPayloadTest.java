package kr.inuappcenterportal.inuportal.firebase;

import com.google.api.core.ApiFutures;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {FcmTestAsyncConfig.class, FcmService.class, FcmTransactionService.class})
class FcmPayloadTest {

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
    @DisplayName("추적 알림 payload에 수신자 공통 fcmMessageId가 실린다")
    void includesFcmMessageIdInPayload() {
        stubSuccessfulSend();

        fcmService.dispatchTrackedNotification(dispatch());

        Map<String, String> data = capturedData();

        // 클라이언트는 이 값으로 읽음 처리 API를 호출한다.
        assertThat(data).containsEntry("fcmMessageId", "1234");
        assertThat(data)
                .containsEntry("type", "POST_REPLY")
                .containsEntry("targetId", "55")
                .containsEntry("path", "/posts/55");
        // 개인별 식별자는 payload에 담기지 않는다. 서버가 인증 정보로 특정한다.
        assertThat(data).doesNotContainKey("memberFcmMessageId");
    }

    @Test
    @DisplayName("알림함에 남지 않는 발송에는 fcmMessageId를 넣지 않는다")
    void omitsFcmMessageIdForUntrackedSend() {
        stubSuccessfulSend();
        when(fcmTokenRepository.findFcmTokensByMemberIds(List.of(69L)))
                .thenReturn(List.of(new kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken(
                        69L, "sample_token_69", "iphone")));

        fcmService.sendUntrackedNotification(List.of(69L), "제목", "본문");

        // 알림함 행이 없으므로 읽음 처리할 대상도 없다.
        assertThat(capturedData()).doesNotContainKey("fcmMessageId");
    }

    private void stubSuccessfulSend() {
        SendResponse successResponse = mock(SendResponse.class);
        when(successResponse.isSuccessful()).thenReturn(true);

        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getResponses()).thenReturn(List.of(successResponse));

        when(firebaseMessaging.sendEachForMulticastAsync(any(MulticastMessage.class)))
                .thenReturn(ApiFutures.immediateFuture(batchResponse));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> capturedData() {
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(firebaseMessaging).sendEachForMulticastAsync(captor.capture());
        Map<String, String> data =
                (Map<String, String>) ReflectionTestUtils.getField(captor.getValue(), "data");
        // data 블록에 아무것도 담기지 않으면 null이다.
        return data == null ? Map.of() : data;
    }

    private TrackedNotificationDispatch dispatch() {
        Map<String, Long> tokenAndMemberId = new LinkedHashMap<>();
        tokenAndMemberId.put("sample_token_69", 69L);
        return new TrackedNotificationDispatch(
                1234L, tokenAndMemberId, "제목", "본문",
                FcmMessageType.POST_REPLY, 55L, "/posts/55");
    }
}
