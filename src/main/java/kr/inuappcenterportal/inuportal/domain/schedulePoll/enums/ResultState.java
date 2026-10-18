package kr.inuappcenterportal.inuportal.domain.schedulePoll.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResultState {
    LATEST("최신"),
    STALE("이전 버전");

    private final String description;
}
