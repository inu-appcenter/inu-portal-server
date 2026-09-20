package kr.inuappcenterportal.inuportal.domain.agent.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CampusWatchStatus {
    ACTIVE("감시 중"),
    NOTIFIED("알림 완료"),
    EXPIRED("시간 만료"),
    CANCELLED("사용자 취소");

    private final String description;
}
