package kr.inuappcenterportal.inuportal.domain.firebase.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.model.MemberFcmMessage;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record NotificationResponse(

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
        LocalDate createDate

) {
    public static NotificationResponse from(MemberFcmMessage memberFcmMessage, FcmMessage fcmMessage) {
        return new NotificationResponse(fcmMessage.getId(), memberFcmMessage.getMemberId(),
                fcmMessage.getTitle(), fcmMessage.getBody(), memberFcmMessage.getFcmMessageType(), memberFcmMessage.getCreateDate());
    }
}
