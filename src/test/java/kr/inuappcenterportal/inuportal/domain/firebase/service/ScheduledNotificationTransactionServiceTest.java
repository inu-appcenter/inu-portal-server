package kr.inuappcenterportal.inuportal.domain.firebase.service;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.AdminNotificationSubFilter;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.AdminNotificationTargetType;
import kr.inuappcenterportal.inuportal.domain.firebase.model.ScheduledNotification;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.ScheduledNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledNotificationTransactionServiceTest {

    @Mock
    ScheduledNotificationRepository scheduledNotificationRepository;

    ScheduledNotificationTransactionService txService;

    @BeforeEach
    void setUp() {
        txService = new ScheduledNotificationTransactionService(scheduledNotificationRepository);
    }

    @Test
    void leaseTranslatesRowCountToBoolean() {
        when(scheduledNotificationRepository.leaseForDispatch(1L)).thenReturn(1);
        when(scheduledNotificationRepository.leaseForDispatch(2L)).thenReturn(0);

        assertThat(txService.lease(1L)).isTrue();
        assertThat(txService.lease(2L)).isFalse();
    }

    @Test
    void cancelIfScheduledTranslatesRowCountToBoolean() {
        when(scheduledNotificationRepository.cancelIfScheduled(1L)).thenReturn(1);
        when(scheduledNotificationRepository.cancelIfScheduled(2L)).thenReturn(0);

        assertThat(txService.cancelIfScheduled(1L)).isTrue();
        assertThat(txService.cancelIfScheduled(2L)).isFalse();
    }

    @Test
    void markSentUpdatesFoundEntity() {
        ScheduledNotification s = scheduledNotification();
        when(scheduledNotificationRepository.findById(1L)).thenReturn(Optional.of(s));

        txService.markSent(1L, 99L);

        assertThat(s.getFcmMessageId()).isEqualTo(99L);
    }

    @Test
    void markSentIsNoOpWhenEntityMissing() {
        when(scheduledNotificationRepository.findById(1L)).thenReturn(Optional.empty());

        txService.markSent(1L, 99L);
        // 예외 없이 조용히 지나가면 충분하다.
    }

    private ScheduledNotification scheduledNotification() {
        ScheduledNotification s = ScheduledNotification.builder()
                .title("t").content("c").targetType(AdminNotificationTargetType.ALL)
                .subFilter(AdminNotificationSubFilter.NONE).requestPayload("{}")
                .scheduledAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(s, "id", 1L);
        return s;
    }
}
