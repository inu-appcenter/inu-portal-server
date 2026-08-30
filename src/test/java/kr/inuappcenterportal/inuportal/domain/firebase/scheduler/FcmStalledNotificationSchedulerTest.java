package kr.inuappcenterportal.inuportal.domain.firebase.scheduler;

import kr.inuappcenterportal.inuportal.domain.firebase.dto.TrackedNotificationDispatch;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmSendStatus;
import kr.inuappcenterportal.inuportal.domain.firebase.event.TrackedNotificationDispatchEvent;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmStalledNotificationSchedulerTest {

    @Mock
    FcmMessageRepository fcmMessageRepository;
    @Mock
    MemberFcmMessageRepository memberFcmMessageRepository;
    @Mock
    FcmTokenRepository fcmTokenRepository;
    @Mock
    ApplicationEventPublisher eventPublisher;

    FcmStalledNotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new FcmStalledNotificationScheduler(
                fcmMessageRepository, memberFcmMessageRepository, fcmTokenRepository, eventPublisher);
    }

    @Test
    void disabledSchedulerNeverTouchesRepositories() {
        enable(scheduler, false, "2026-08-30T00:00:00");

        scheduler.recoverStalledNotifications();

        verifyNoInteractions(fcmMessageRepository, memberFcmMessageRepository, fcmTokenRepository, eventPublisher);
    }

    @Test
    void enablingWithoutNotBeforeFailsFastAtStartup() {
        enable(scheduler, true, "");

        assertThatThrownBy(() -> scheduler.validateConfig())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not-before");
    }

    @Test
    void maxAgeNotGreaterThanStalledAfterFailsFastAtStartup() {
        enable(scheduler, true, "2026-08-30T00:00:00");
        ReflectionTestUtils.setField(scheduler, "stalledAfterMinutes", 30L);
        ReflectionTestUtils.setField(scheduler, "maxAgeMinutes", 30L);

        assertThatThrownBy(() -> scheduler.validateConfig())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max-age-minutes");
    }

    @Test
    void leaseFailureSkipsRepublishWithoutPublishingEvent() {
        enableValid(scheduler);
        FcmMessage candidate = pendingMessageWithId(1L);
        when(fcmMessageRepository.findStalledCandidates(eq(FcmSendStatus.PENDING), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(candidate));
        // 다른 인스턴스(또는 이미 시작된 원래 발송)가 먼저 선점했다고 가정한다.
        when(fcmMessageRepository.leasePendingForRecovery(1L)).thenReturn(0);

        scheduler.republishPendingNotifications(java.time.LocalDateTime.now());

        verify(fcmMessageRepository).leasePendingForRecovery(1L);
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(memberFcmMessageRepository);
    }

    @Test
    void leaseSuccessRepublishesWithRestoredPayload() {
        enableValid(scheduler);
        FcmMessage candidate = trackedMessage(2L, "title", "body", 5L, "/board/5");
        when(fcmMessageRepository.findStalledCandidates(eq(FcmSendStatus.PENDING), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(candidate));
        when(fcmMessageRepository.leasePendingForRecovery(2L)).thenReturn(1);
        when(memberFcmMessageRepository.findMemberIdsByFcmMessageId(2L)).thenReturn(List.of(10L, 11L));
        when(memberFcmMessageRepository.findDistinctTypesByFcmMessageId(2L)).thenReturn(List.of(FcmMessageType.POST_REPLY));
        when(fcmTokenRepository.findFcmTokensByMemberIds(List.of(10L, 11L)))
                .thenReturn(List.of(new FcmToken(10L, "token-10", "android")));

        scheduler.republishPendingNotifications(java.time.LocalDateTime.now());

        ArgumentCaptor<TrackedNotificationDispatchEvent> captor =
                ArgumentCaptor.forClass(TrackedNotificationDispatchEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        TrackedNotificationDispatch dispatch = captor.getValue().dispatch();
        assertThat(dispatch.fcmMessageId()).isEqualTo(2L);
        assertThat(dispatch.path()).isEqualTo("/board/5");
        assertThat(dispatch.type()).isEqualTo(FcmMessageType.POST_REPLY);
        assertThat(dispatch.tokenAndMemberId()).containsEntry("token-10", 10L);
    }

    @Test
    void ambiguousTypeSkipsRecoveryInsteadOfGuessing() {
        enableValid(scheduler);
        FcmMessage candidate = trackedMessage(3L, "title", "body", null, null);
        when(fcmMessageRepository.findStalledCandidates(eq(FcmSendStatus.PENDING), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(candidate));
        when(fcmMessageRepository.leasePendingForRecovery(3L)).thenReturn(1);
        when(memberFcmMessageRepository.findMemberIdsByFcmMessageId(3L)).thenReturn(List.of(20L));
        // 서로 다른 type이 섞여 있으면(모델링상 있어서는 안 되지만) 임의로 하나를 고르지 않는다.
        when(memberFcmMessageRepository.findDistinctTypesByFcmMessageId(3L))
                .thenReturn(List.of(FcmMessageType.POST_REPLY, FcmMessageType.GENERAL));

        scheduler.republishPendingNotifications(java.time.LocalDateTime.now());

        verifyNoInteractions(eventPublisher);
        verify(fcmTokenRepository, never()).findFcmTokensByMemberIds(any());
    }

    @Test
    void noInboxRowsSettlesAsNoTargetInsteadOfLoopingForever() {
        enableValid(scheduler);
        FcmMessage candidate = trackedMessage(4L, "title", "body", null, null);
        when(fcmMessageRepository.findStalledCandidates(eq(FcmSendStatus.PENDING), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(candidate));
        when(fcmMessageRepository.leasePendingForRecovery(4L)).thenReturn(1);
        when(memberFcmMessageRepository.findMemberIdsByFcmMessageId(4L)).thenReturn(List.of());

        scheduler.republishPendingNotifications(java.time.LocalDateTime.now());

        verifyNoInteractions(eventPublisher);
        // 알림함 행이 아예 없으면 다음 tick에도 안 걸리도록 즉시 종결(NO_TARGET)돼야 한다.
        assertThat(candidate.getSendStatus()).isEqualTo(FcmSendStatus.NO_TARGET);
        verify(memberFcmMessageRepository, never()).findDistinctTypesByFcmMessageId(anyLong());
    }

    @Test
    void finalizeStalledProcessingKeepsAccumulatedCountsInsteadOfResettingThem() {
        enableValid(scheduler);
        FcmMessage stuck = FcmMessage.builder().title("t").body("b").build();
        stuck.markPending(10);
        stuck.markProcessing();
        stuck.incrementDeliveryResult(7, 1); // 부분 발송된 상태에서 멈춤
        when(fcmMessageRepository.findStalledCandidates(eq(FcmSendStatus.PROCESSING), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(stuck));

        scheduler.finalizeStalledProcessing(java.time.LocalDateTime.now());

        // completeProcessing()은 기존 집계를 리셋하지 않고 그대로 확정해야 한다 (#431 부수 결함).
        assertThat(stuck.getSendCount()).isEqualTo(7);
        assertThat(stuck.getFailureCount()).isEqualTo(1);
        assertThat(stuck.getSendStatus()).isEqualTo(FcmSendStatus.PARTIAL_FAILURE);
    }

    private void enableValid(FcmStalledNotificationScheduler scheduler) {
        enable(scheduler, true, "2026-08-30T00:00:00");
        scheduler.validateConfig();
    }

    private void enable(FcmStalledNotificationScheduler scheduler, boolean enabled, String notBeforeRaw) {
        ReflectionTestUtils.setField(scheduler, "recoveryEnabled", enabled);
        ReflectionTestUtils.setField(scheduler, "notBeforeRaw", notBeforeRaw);
        ReflectionTestUtils.setField(scheduler, "stalledAfterMinutes", 10L);
        ReflectionTestUtils.setField(scheduler, "maxAgeMinutes", 60L);
        ReflectionTestUtils.setField(scheduler, "maxPerRun", 20);
    }

    private FcmMessage pendingMessageWithId(Long id) {
        FcmMessage message = FcmMessage.builder().title("t").body("b").build();
        message.markPending(1);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }

    private FcmMessage trackedMessage(Long id, String title, String body, Long targetId, String path) {
        FcmMessage message = FcmMessage.builder().title(title).body(body).targetId(targetId).path(path).build();
        message.markPending(1);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }
}
