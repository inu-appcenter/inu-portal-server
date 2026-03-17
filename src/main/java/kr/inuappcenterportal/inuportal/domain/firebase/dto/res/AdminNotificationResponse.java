package kr.inuappcenterportal.inuportal.domain.firebase.dto.res;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmSendStatus;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;

public record AdminNotificationResponse(
        Long id,
        String title,
        String body,
        int targetCount,
        int sendCount,
        int failureCount,
        FcmSendStatus status
) {
    public static AdminNotificationResponse of(FcmMessage fcmMessage) {
        return new AdminNotificationResponse(
                fcmMessage.getId(),
                fcmMessage.getTitle(),
                fcmMessage.getBody(),
                fcmMessage.getTargetCount(),
                fcmMessage.getSendCount(),
                fcmMessage.getFailureCount(),
                fcmMessage.getSendStatus()
        );
    }
}
