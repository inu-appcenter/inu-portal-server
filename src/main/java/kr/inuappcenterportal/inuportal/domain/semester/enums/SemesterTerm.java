package kr.inuappcenterportal.inuportal.domain.semester.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SemesterTerm {
    FIRST("1학기"),
    SUMMER("여름학기"),
    SECOND("2학기"),
    WINTER("겨울학기");

    private final String displayName;
}
