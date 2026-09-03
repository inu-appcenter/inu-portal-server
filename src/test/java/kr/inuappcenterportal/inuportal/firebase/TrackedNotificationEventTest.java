package kr.inuappcenterportal.inuportal.firebase;

import com.google.firebase.messaging.FirebaseMessaging;
import kr.inuappcenterportal.inuportal.config.FcmTestAsyncConfig;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.event.TrackedNotificationDispatchEvent;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RecordApplicationEvents
@SpringBootTest(classes = {FcmTestAsyncConfig.class, FcmService.class, FcmTransactionService.class})
class TrackedNotificationEventTest {

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

    @Autowired
    private ApplicationEvents applicationEvents;

    @Test
    @DisplayName("알림 저장 시 발송 이벤트만 발행하고, 저장 시점에는 FCM을 호출하지 않는다")
    void publishesEventWithoutSendingDuringSave() {
        when(fcmTokenRepository.findFcmTokensByMemberIds(List.of(69L)))
                .thenReturn(List.of(new FcmToken(69L, "sample_token_69", "iphone")));
        when(fcmMessageRepository.saveAndFlush(any(FcmMessage.class))).thenAnswer(invocation -> {
            FcmMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 1L);
            return message;
        });

        fcmService.prepareTrackedNotification(
                List.of(69L), "제목", "본문", FcmMessageType.POST_REPLY, 55L, "/posts/55");

        // 발송은 커밋 이후 FcmEventListener가 담당한다. 저장 트랜잭션 안에서는 나가지 않는다.
        verifyNoInteractions(firebaseMessaging);

        List<TrackedNotificationDispatchEvent> events =
                applicationEvents.stream(TrackedNotificationDispatchEvent.class).toList();

        assertThat(events).hasSize(1);
        assertThat(events.get(0).dispatch().fcmMessageId()).isEqualTo(1L);
        assertThat(events.get(0).dispatch().path()).isEqualTo("/posts/55");
    }

    @Test
    @DisplayName("대상 회원이 없으면 이벤트를 발행하지 않는다")
    void doesNotPublishWhenNoMember() {
        fcmService.prepareTrackedNotification(
                List.of(), "제목", "본문", FcmMessageType.POST_REPLY, 55L, "/posts/55");

        assertThat(applicationEvents.stream(TrackedNotificationDispatchEvent.class)).isEmpty();
        verifyNoInteractions(firebaseMessaging);
    }
}
