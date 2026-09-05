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

        @Schema(description = """
                알림을 눌렀을 때 이동할 위치. 푸시 payload의 data.path와 같은 값이라
                시스템 알림 탭과 알림함 탭이 같은 화면으로 간다.
                포털 내부 경로("/home/tips/12")이거나 학교 공지처럼 외부 링크
                (https://www.inu.ac.kr/...)일 수 있고, path를 싣지 않고 발송된
                알림(과거 이력 포함)은 null이다.""",
                example = "/home/tips/12")
        String path,

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
                fcmMessage.getPath(),
                memberFcmMessage.isRead()
        );
    }
}
