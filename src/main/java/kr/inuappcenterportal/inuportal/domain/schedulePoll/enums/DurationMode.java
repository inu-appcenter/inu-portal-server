package kr.inuappcenterportal.inuportal.domain.schedulePoll.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DurationMode {
    FIXED("고정 길이"),
    AT_LEAST("2시간 이상");

    private final String description;
}
