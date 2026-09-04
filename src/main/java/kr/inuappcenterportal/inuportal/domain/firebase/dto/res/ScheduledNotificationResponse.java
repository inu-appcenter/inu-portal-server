package kr.inuappcenterportal.inuportal.domain.firebase.dto.res;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.AdminNotificationSubFilter;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.AdminNotificationTargetType;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.ScheduledNotificationStatus;
import kr.inuappcenterportal.inuportal.domain.firebase.model.ScheduledNotification;

import java.time.LocalDateTime;

public record ScheduledNotificationResponse(
        Long id,
        String title,
        String content,
        String path,
        AdminNotificationTargetType targetType,
        AdminNotificationSubFilter subFilter,
        LocalDateTime scheduledAt,
        ScheduledNotificationStatus status,
        Long fcmMessageId,
        String failureReason
) {
    public static ScheduledNotificationResponse of(ScheduledNotification scheduledNotification) {
        return new ScheduledNotificationResponse(
                scheduledNotification.getId(),
                scheduledNotification.getTitle(),
                scheduledNotification.getContent(),
                scheduledNotification.getPath(),
                scheduledNotification.getTargetType(),
                scheduledNotification.getSubFilter(),
                scheduledNotification.getScheduledAt(),
                scheduledNotification.getStatus(),
                scheduledNotification.getFcmMessageId(),
                scheduledNotification.getFailureReason()
        );
    }
}
