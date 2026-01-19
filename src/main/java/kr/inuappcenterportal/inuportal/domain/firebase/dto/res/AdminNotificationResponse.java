package kr.inuappcenterportal.inuportal.domain.firebase.dto.res;

import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;

public record AdminNotificationResponse(

        Long id,

        String title,

        String body,

        int sendCount

) {
    public static AdminNotificationResponse of(FcmMessage fcmMessage) {
        return new AdminNotificationResponse(
                fcmMessage.getId(),
                fcmMessage.getTitle(),
                fcmMessage.getBody(),
                fcmMessage.getSendCount()
        );
    }
}
