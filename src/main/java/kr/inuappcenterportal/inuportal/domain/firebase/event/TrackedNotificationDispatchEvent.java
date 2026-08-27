package kr.inuappcenterportal.inuportal.domain.firebase.event;

import kr.inuappcenterportal.inuportal.domain.firebase.dto.TrackedNotificationDispatch;

/**
 * 알림함에 이력을 남기는 푸시 발송 요청 이벤트.
 * <p>
 * 트랜잭션 커밋 이후에 비동기로 처리되므로 영속성 컨텍스트가 없다.
 * 따라서 엔티티가 아닌 값(id, 토큰 문자열)만 담는다.
 */
public record TrackedNotificationDispatchEvent(TrackedNotificationDispatch dispatch) {
}
