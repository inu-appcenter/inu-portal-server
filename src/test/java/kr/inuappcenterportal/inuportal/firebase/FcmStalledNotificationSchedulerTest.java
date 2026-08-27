package kr.inuappcenterportal.inuportal.firebase;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmSendStatus;
import kr.inuappcenterportal.inuportal.domain.firebase.event.TrackedNotificationDispatchEvent;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.scheduler.FcmStalledNotificationScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FcmStalledNotificationSchedulerTest {

    @Mock
    private FcmMessageRepository fcmMessageRepository;

    @Mock
    private MemberFcmMessageRepository memberFcmMessageRepository;

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FcmStalledNotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "stalledAfterMinutes", 10L);
        ReflectionTestUtils.setField(scheduler, "maxPerRun", 20);
    }

    @Test
    @DisplayName("발송이 시작되지 못한 PENDING 알림은 원래 payload 그대로 다시 발행된다")
    void republishesPendingNotification() {
        FcmMessage stalled = fcmMessage(1L, "제목", "본문", 55L, "/posts/55");

        when(fcmMessageRepository.findAllBySendStatusAndModifiedDateBefore(eq(FcmSendStatus.PENDING), any()))
                .thenReturn(List.of(stalled));
        when(fcmMessageRepository.findAllBySendStatusAndModifiedDateBefore(eq(FcmSendStatus.PROCESSING), any()))
                .thenReturn(List.of());
        when(memberFcmMessageRepository.findMemberIdsByFcmMessageId(1L)).thenReturn(List.of(69L));
        when(memberFcmMessageRepository.findTypesByFcmMessageId(1L)).thenReturn(List.of(FcmMessageType.POST_REPLY));
        when(fcmTokenRepository.findFcmTokensByMemberIds(List.of(69L)))
                .thenReturn(List.of(new FcmToken(69L, "sample_token_69", "iphone")));

        scheduler.recoverStalledNotifications();

        ArgumentCaptor<TrackedNotificationDispatchEvent> captor =
                ArgumentCaptor.forClass(TrackedNotificationDispatchEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        var dispatch = captor.getValue().dispatch();
        assertThat(dispatch.fcmMessageId()).isEqualTo(1L);
        assertThat(dispatch.title()).isEqualTo("제목");
        assertThat(dispatch.body()).isEqualTo("본문");
        assertThat(dispatch.targetId()).isEqualTo(55L);
        // path가 FcmMessage에 저장되므로 복구 시에도 라우팅 정보가 유지된다.
        assertThat(dispatch.path()).isEqualTo("/posts/55");
        assertThat(dispatch.type()).isEqualTo(FcmMessageType.POST_REPLY);
        assertThat(dispatch.tokenAndMemberId()).containsEntry("sample_token_69", 69L);
    }

    @Test
    @DisplayName("PROCESSING에 멈춘 알림은 재발송하지 않고 실패로만 확정한다")
    void doesNotResendStalledProcessing() {
        FcmMessage stalled = fcmMessage(2L, "제목", "본문", null, null);
        stalled.markPending(3);
        stalled.markProcessing();

        when(fcmMessageRepository.findAllBySendStatusAndModifiedDateBefore(eq(FcmSendStatus.PENDING), any()))
                .thenReturn(List.of());
        when(fcmMessageRepository.findAllBySendStatusAndModifiedDateBefore(eq(FcmSendStatus.PROCESSING), any()))
                .thenReturn(List.of(stalled));

        scheduler.recoverStalledNotifications();

        // 어디까지 나갔는지 알 수 없으므로 재발송하면 중복 푸시가 된다.
        verify(eventPublisher, never()).publishEvent(any(TrackedNotificationDispatchEvent.class));
        assertThat(stalled.getSendStatus()).isEqualTo(FcmSendStatus.FAILED);
    }

    @Test
    @DisplayName("수신 대상 토큰이 남아있지 않으면 재발행하지 않는다")
    void skipsWhenNoTargetRemains() {
        FcmMessage stalled = fcmMessage(3L, "제목", "본문", null, null);

        when(fcmMessageRepository.findAllBySendStatusAndModifiedDateBefore(eq(FcmSendStatus.PENDING), any()))
                .thenReturn(List.of(stalled));
        when(fcmMessageRepository.findAllBySendStatusAndModifiedDateBefore(eq(FcmSendStatus.PROCESSING), any()))
                .thenReturn(List.of());
        when(memberFcmMessageRepository.findMemberIdsByFcmMessageId(3L)).thenReturn(List.of(69L));
        when(fcmTokenRepository.findFcmTokensByMemberIds(List.of(69L))).thenReturn(List.of());

        scheduler.recoverStalledNotifications();

        verify(eventPublisher, never()).publishEvent(any(TrackedNotificationDispatchEvent.class));
    }

    private FcmMessage fcmMessage(Long id, String title, String body, Long targetId, String path) {
        FcmMessage message = FcmMessage.builder()
                .title(title)
                .body(body)
                .targetId(targetId)
                .path(path)
                .build();
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "modifiedDate", LocalDateTime.now().minusMinutes(30));
        return message;
    }
}
