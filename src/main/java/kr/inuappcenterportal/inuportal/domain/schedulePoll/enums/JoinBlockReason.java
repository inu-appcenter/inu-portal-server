package kr.inuappcenterportal.inuportal.domain.schedulePoll.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 링크 미리보기에서 입력할 수 없는 이유
@Getter
@RequiredArgsConstructor
public enum JoinBlockReason {
    CLOSED("끝났거나 마감이 지남"),
    BLOCKED("개설자와 차단 관계"),
    FULL("참여자 10명");

    private final String description;
}
