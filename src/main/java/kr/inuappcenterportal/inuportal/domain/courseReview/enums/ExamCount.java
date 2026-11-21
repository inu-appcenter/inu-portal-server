package kr.inuappcenterportal.inuportal.domain.courseReview.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExamCount {
    TWICE("2회"),
    ONCE("1회"),
    REPLACEMENT("과제대체"),
    NONE("없음");

    private final String description;
}
