package kr.inuappcenterportal.inuportal.domain.firebase.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.AdminNotificationDispatch;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.AdminNotificationRequest;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.AdminNotificationSubFilter;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.AdminNotificationTargetType;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.ScheduledNotificationStatus;
import kr.inuappcenterportal.inuportal.domain.firebase.model.ScheduledNotification;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.ScheduledNotificationRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.firebase.service.ScheduledNotificationTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledNotificationListenerTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-05T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    ScheduledNotificationRepository scheduledNotificationRepository;
    @Mock
    ScheduledNotificationTransactionService txService;
    @Mock
    FcmService fcmService;

    ObjectMapper objectMapper;
    ScheduledNotificationListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        listener = new ScheduledNotificationListener(
                scheduledNotificationRepository, txService, fcmService, objectMapper, FIXED_CLOCK);
        ReflectionTestUtils.setField(listener, "maxDelayMinutes", 30L);
    }

    @Test
    void notInDispatchingStateIsSkippedDefensively() {
        ScheduledNotification s = scheduledNotification(1L, LocalDateTime.now(FIXED_CLOCK));
        // lease를 안 거친 채로 이벤트가 온 방어적 케이스: 상태가 SCHEDULED로 남아 있다.
        when(scheduledNotificationRepository.findById(1L)).thenReturn(Optional.of(s));

        listener.onDue(new ScheduledNotificationDueEvent(1L));

        verify(fcmService, never()).prepareAdminNotification(any());
        verify(txService, never()).markSent(any(), any());
    }

    @Test
    void expiredScheduleIsMarkedExpiredWithoutSending() {
        ScheduledNotification s = scheduledNotification(2L, LocalDateTime.now(FIXED_CLOCK).minusMinutes(31));
        ReflectionTestUtils.setField(s, "status", ScheduledNotificationStatus.DISPATCHING);
        when(scheduledNotificationRepository.findById(2L)).thenReturn(Optional.of(s));

        listener.onDue(new ScheduledNotificationDueEvent(2L));

        verify(txService).markExpired(2L);
        verify(fcmService, never()).prepareAdminNotification(any());
    }

    @Test
    void dueScheduleIsDispatchedThroughExistingAdminFlow() {
        AdminNotificationRequest request = new AdminNotificationRequest(
                AdminNotificationTargetType.ALL, AdminNotificationSubFilter.NONE,
                null, null, null, "title", "content", "/board/1", null);
        String payload;
        try {
            payload = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ScheduledNotification s = scheduledNotification(3L, LocalDateTime.now(FIXED_CLOCK));
        ReflectionTestUtils.setField(s, "status", ScheduledNotificationStatus.DISPATCHING);
        ReflectionTestUtils.setField(s, "requestPayload", payload);
        when(scheduledNotificationRepository.findById(3L)).thenReturn(Optional.of(s));

        AdminNotificationDispatch dispatch = new AdminNotificationDispatch(
                100L, "title", "content", Map.of("token", 1L), java.util.List.of(1L), "/board/1");
        when(fcmService.prepareAdminNotification(any())).thenReturn(dispatch);

        listener.onDue(new ScheduledNotificationDueEvent(3L));

        verify(txService).markSent(3L, 100L);
        verify(fcmService).sendToMembers(dispatch);
        verify(txService, never()).markFailed(any(), anyString());
    }

    @Test
    void malformedPayloadMarksFailedInsteadOfThrowing() {
        ScheduledNotification s = scheduledNotification(4L, LocalDateTime.now(FIXED_CLOCK));
        ReflectionTestUtils.setField(s, "status", ScheduledNotificationStatus.DISPATCHING);
        ReflectionTestUtils.setField(s, "requestPayload", "not-json");
        when(scheduledNotificationRepository.findById(4L)).thenReturn(Optional.of(s));

        listener.onDue(new ScheduledNotificationDueEvent(4L));

        verify(txService, times(1)).markFailed(org.mockito.ArgumentMatchers.eq(4L), anyString());
        verify(fcmService, never()).sendToMembers(any());
    }

    private ScheduledNotification scheduledNotification(Long id, LocalDateTime scheduledAt) {
        ScheduledNotification s = ScheduledNotification.builder()
                .title("t").content("c").targetType(AdminNotificationTargetType.ALL)
                .subFilter(AdminNotificationSubFilter.NONE).requestPayload("{}").scheduledAt(scheduledAt)
                .build();
        ReflectionTestUtils.setField(s, "id", id);
        return s;
    }
}
