package kr.inuappcenterportal.inuportal.domain.firebase.event;

import kr.inuappcenterportal.inuportal.domain.firebase.dto.TrackedNotificationDispatch;

/**
 * {@code prepareTrackedNotification}이 저장 트랜잭션 커밋 이후 실제 FCM 발송을
 * 트리거하기 위해 발행하는 이벤트. {@link FcmEventListener}가 AFTER_COMMIT에서 받는다.
 */
public record TrackedNotificationDispatchEvent(TrackedNotificationDispatch dispatch) {
}
