package kr.inuappcenterportal.inuportal.domain.course.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TargetGrade {
    ALL("전학년"),
    FIRST("1학년"),
    SECOND("2학년"),
    THIRD("3학년"),
    FOURTH("4학년"),
    UNKNOWN("미정");

    private final String displayName;
}
