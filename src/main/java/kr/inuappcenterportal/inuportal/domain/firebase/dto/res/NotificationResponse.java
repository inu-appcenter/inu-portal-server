package kr.inuappcenterportal.inuportal.domain.firebase.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.model.MemberFcmMessage;

import java.time.LocalDateTime; // LocalDate 대신 LocalDateTime 사용

public record NotificationResponse(

        @Schema(description = "회원 알림 이력 Id")
        Long memberFcmMessageId,

        @Schema(description = "알림 Id")
        Long fcmMessageId,

        @Schema(description = "회원 Id")
        Long memberId,

        @Schema(description = "알림 제목")
        String title,

        @Schema(description = "알림 내용")
        String body,

        @Schema(description = "알림 타입")
        FcmMessageType type,

        @Schema(description = "알림 생성 시간")
        LocalDateTime createDate,

        @Schema(description = "이동 대상 Id (noticeId, friendId 등)")
        Long targetId,

        @Schema(description = "알림 읽음 여부")
        boolean isRead

) {
    public static NotificationResponse from(MemberFcmMessage memberFcmMessage, FcmMessage fcmMessage) {
        return new NotificationResponse(
                memberFcmMessage.getId(),
                fcmMessage.getId(),
                memberFcmMessage.getMemberId(),
                fcmMessage.getTitle(),
                fcmMessage.getBody(),
                memberFcmMessage.getFcmMessageType(),
                memberFcmMessage.getCreateDate(),
                fcmMessage.getTargetId(),
                memberFcmMessage.isRead()
        );
    }
}
