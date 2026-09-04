package kr.inuappcenterportal.inuportal.domain.firebase.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.AdminNotificationRequest;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.AdminNotificationSubFilter;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.AdminNotificationTargetType;
import kr.inuappcenterportal.inuportal.domain.firebase.model.ScheduledNotification;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.ScheduledNotificationRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledNotificationServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-05T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    ScheduledNotificationRepository scheduledNotificationRepository;
    @Mock
    ScheduledNotificationTransactionService txService;

    ScheduledNotificationService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new ScheduledNotificationService(scheduledNotificationRepository, txService, objectMapper, FIXED_CLOCK);
        ReflectionTestUtils.setField(service, "maxAdvanceDays", 90L);
    }

    @Test
    void reserveSavesPayloadAndTargetMetadata() {
        AdminNotificationRequest request = new AdminNotificationRequest(
                null, null, null, null, null, "title", "content", "/board/1",
                LocalDateTime.now(FIXED_CLOCK).plusDays(1));
        when(scheduledNotificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScheduledNotification saved = service.reserve(request);

        ArgumentCaptor<ScheduledNotification> captor = ArgumentCaptor.forClass(ScheduledNotification.class);
        verify(scheduledNotificationRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("title");
        assertThat(captor.getValue().getTargetType()).isEqualTo(AdminNotificationTargetType.ALL);
        assertThat(captor.getValue().getSubFilter()).isEqualTo(AdminNotificationSubFilter.NONE);
        assertThat(captor.getValue().getRequestPayload()).contains("\"title\":\"title\"");
        assertThat(saved).isSameAs(captor.getValue());
    }

    @Test
    void reserveRejectsNullScheduledAt() {
        AdminNotificationRequest request = new AdminNotificationRequest(
                null, null, null, null, null, "title", "content", null, null);

        assertThatThrownBy(() -> service.reserve(request))
                .isInstanceOf(MyException.class);
    }

    @Test
    void reserveRejectsTooFarInTheFuture() {
        AdminNotificationRequest request = new AdminNotificationRequest(
                null, null, null, null, null, "title", "content", null,
                LocalDateTime.now(FIXED_CLOCK).plusDays(91));

        assertThatThrownBy(() -> service.reserve(request))
                .isInstanceOf(MyException.class);
    }

    @Test
    void cancelDelegatesToConditionalUpdate() {
        ScheduledNotification s = ScheduledNotification.builder()
                .title("t").content("c").targetType(AdminNotificationTargetType.ALL)
                .subFilter(AdminNotificationSubFilter.NONE).requestPayload("{}")
                .scheduledAt(LocalDateTime.now(FIXED_CLOCK)).build();
        when(scheduledNotificationRepository.findById(1L)).thenReturn(Optional.of(s));
        when(txService.cancelIfScheduled(1L)).thenReturn(true);

        service.cancel(1L);

        verify(txService).cancelIfScheduled(1L);
    }

    @Test
    void cancelRejectsWhenAlreadyDispatching() {
        ScheduledNotification s = ScheduledNotification.builder()
                .title("t").content("c").targetType(AdminNotificationTargetType.ALL)
                .subFilter(AdminNotificationSubFilter.NONE).requestPayload("{}")
                .scheduledAt(LocalDateTime.now(FIXED_CLOCK)).build();
        when(scheduledNotificationRepository.findById(1L)).thenReturn(Optional.of(s));
        when(txService.cancelIfScheduled(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.cancel(1L))
                .isInstanceOf(MyException.class)
                .satisfies(e -> assertThat(((MyException) e).getErrorCode())
                        .isEqualTo(MyErrorCode.SCHEDULED_NOTIFICATION_ALREADY_DISPATCHING));
    }

    @Test
    void cancelRejectsUnknownId() {
        when(scheduledNotificationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(1L))
                .isInstanceOf(MyException.class);
    }
}
