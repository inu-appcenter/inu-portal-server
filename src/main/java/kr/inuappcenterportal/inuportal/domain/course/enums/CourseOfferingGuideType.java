package kr.inuappcenterportal.inuportal.domain.course.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseOfferingGuideType {
    BASIC_GENERAL("기초교양"),
    CORE_DEEP_GENERAL("핵심,심화교양"),
    BASIC_SCIENCE_ENGINEERING("기초과학,공학"),
    TEACHING("교직"),
    LINKED_MAJOR("연계전공"),
    COMMON_MILITARY("기타[일반선,군사학]"),
    MAJOR("학과별 전공"),
    UNKNOWN("알 수 없음");

    private final String description;
}
