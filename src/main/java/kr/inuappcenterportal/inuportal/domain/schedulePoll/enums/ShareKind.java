package kr.inuappcenterportal.inuportal.domain.schedulePoll.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ShareKind {
    INVITE("참여 요청 카드"),
    RESULT("결과 카드");

    private final String description;
}
