package kr.inuappcenterportal.inuportal.domain.firebase.scheduler;

import kr.inuappcenterportal.inuportal.domain.firebase.event.ScheduledNotificationDueEvent;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.ScheduledNotificationRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.ScheduledNotificationTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledNotificationSchedulerTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-05T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    ScheduledNotificationRepository scheduledNotificationRepository;
    @Mock
    ScheduledNotificationTransactionService txService;
    @Mock
    ApplicationEventPublisher eventPublisher;

    ScheduledNotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ScheduledNotificationScheduler(
                scheduledNotificationRepository, txService, eventPublisher, FIXED_CLOCK);
        ReflectionTestUtils.setField(scheduler, "maxPerRun", 20);
        ReflectionTestUtils.setField(scheduler, "stalledAfterMinutes", 5L);
    }

    @Test
    void disabledSchedulerNeverTouchesRepositories() {
        ReflectionTestUtils.setField(scheduler, "scheduleEnabled", false);

        scheduler.dispatchDueNotifications();

        verifyNoInteractions(scheduledNotificationRepository, txService, eventPublisher);
    }

    @Test
    void leaseFailureSkipsPublishingEvent() {
        ReflectionTestUtils.setField(scheduler, "scheduleEnabled", true);
        when(scheduledNotificationRepository.findDueIds(any(), any(), any(Pageable.class))).thenReturn(List.of(1L));
        // 다른 인스턴스가 먼저 선점했거나, 그사이 취소됐다고 가정한다.
        when(txService.lease(eq(1L), any(), any())).thenReturn(false);

        scheduler.dispatchDueNotifications();

        verify(txService).lease(eq(1L), any(), any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void leaseSuccessPublishesDueEvent() {
        ReflectionTestUtils.setField(scheduler, "scheduleEnabled", true);
        when(scheduledNotificationRepository.findDueIds(any(), any(), any(Pageable.class))).thenReturn(List.of(2L, 3L));
        when(txService.lease(eq(2L), any(), any())).thenReturn(true);
        when(txService.lease(eq(3L), any(), any())).thenReturn(false);

        scheduler.dispatchDueNotifications();

        ArgumentCaptor<ScheduledNotificationDueEvent> captor = ArgumentCaptor.forClass(ScheduledNotificationDueEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().scheduledNotificationId()).isEqualTo(2L);
    }

    @Test
    void noDueCandidatesSkipsLeaseAttempts() {
        ReflectionTestUtils.setField(scheduler, "scheduleEnabled", true);
        when(scheduledNotificationRepository.findDueIds(any(), any(), any(Pageable.class))).thenReturn(List.of());

        scheduler.dispatchDueNotifications();

        verifyNoInteractions(txService, eventPublisher);
    }
}
