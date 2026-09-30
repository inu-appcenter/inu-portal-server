package kr.inuappcenterportal.inuportal.domain.dailyBrief.scheduler;

import kr.inuappcenterportal.inuportal.domain.firebase.dto.LiveActivityStartPush;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;

/**
 * 회원 한 명에게 보낼 Daily Brief 알림 한 건. 트랜잭션 밖에서 발송할 수 있도록 엔티티 없이 값만 담는다.
 *
 * @param liveActivity 수업 전 알림일 때만 있다. push-to-start 토큰이 있는 iOS 기기에 Live Activity로 보낸다.
 */
record BriefNotification(
        Long memberId,
        String title,
        String body,
        FcmMessageType type,
        String path,
        LiveActivityStartPush liveActivity
) {
}
