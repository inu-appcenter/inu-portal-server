package kr.inuappcenterportal.inuportal.domain.firebase.dto.res;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmSendStatus;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;

import java.time.LocalDateTime;

/**
 * @param retryableCount 재시도로 다시 보낼 수 있는 회원 수. 전달에 끝내 실패한 회원만 센다.
 *                       0이면 재시도 버튼을 눌러도 보낼 대상이 없다(기능 도입 이전 발송 건 포함).
 * @param retryCount     관리자가 수동 재시도한 횟수.
 * @param lastRetriedAt  마지막 재시도 시각. 재시도한 적 없으면 null.
 */
public record AdminNotificationResponse(
        Long id,
        String title,
        String body,
        int targetCount,
        int sendCount,
        int failureCount,
        FcmSendStatus status,
        int retryableCount,
        int retryCount,
        LocalDateTime lastRetriedAt
) {
    public static AdminNotificationResponse of(FcmMessage fcmMessage) {
        return of(fcmMessage, 0);
    }

    public static AdminNotificationResponse of(FcmMessage fcmMessage, int retryableCount) {
        return new AdminNotificationResponse(
                fcmMessage.getId(),
                fcmMessage.getTitle(),
                fcmMessage.getBody(),
                fcmMessage.getTargetCount(),
                fcmMessage.getSendCount(),
                fcmMessage.getFailureCount(),
                fcmMessage.getSendStatus(),
                retryableCount,
                fcmMessage.getRetryCount(),
                fcmMessage.getLastRetriedAt()
        );
    }
}
