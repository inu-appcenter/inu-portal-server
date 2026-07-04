package kr.inuappcenterportal.inuportal.domain.course.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TargetTerm {
    FIRST("1학기"),
    SECOND("2학기"),
    BOTH("공통"),
    UNKNOWN("미정");

    private final String displayName;
}
