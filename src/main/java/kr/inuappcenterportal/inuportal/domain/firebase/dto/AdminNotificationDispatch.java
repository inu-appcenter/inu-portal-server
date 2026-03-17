package kr.inuappcenterportal.inuportal.domain.firebase.dto;

import java.util.List;
import java.util.Map;

public record AdminNotificationDispatch(
        Long fcmMessageId,
        String title,
        String content,
        Map<String, Long> tokenAndMemberId,
        List<Long> targetMemberIds
) {
    public boolean hasTarget() {
        return !tokenAndMemberId.isEmpty();
    }

    public boolean hasMemberTarget() {
        return !targetMemberIds.isEmpty();
    }

    public int targetCount() {
        return tokenAndMemberId.size();
    }

    public int memberTargetCount() {
        return targetMemberIds.size();
    }
}
