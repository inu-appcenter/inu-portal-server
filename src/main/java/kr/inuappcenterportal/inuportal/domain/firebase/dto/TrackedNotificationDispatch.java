package kr.inuappcenterportal.inuportal.domain.firebase.dto;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;

import java.util.Map;

public record TrackedNotificationDispatch(
        Long fcmMessageId,
        Map<String, Long> tokenAndMemberId,
        String title,
        String body,
        FcmMessageType type,
        Long targetId,
        String path,
        Map<Long, Long> memberFcmMessageIds
) {
}
