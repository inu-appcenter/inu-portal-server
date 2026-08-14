package kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ISU_FLD_NAME {

    CORE_INU_SEMINAR("(핵심)INU세미나"),
    CORE_SCIENCE_TECHNOLOGY("(핵심)과학기술"),
    CORE_SOCIAL_SCIENCE("(핵심)사회"),
    CORE_ARTS_PHYSICAL_EDUCATION("(핵심)예술체육"),
    CORE_FOREIGN_LANGUAGE("(핵심)외국어"),
    CORE_HUMANITIES("(핵심)인문"),

    SCIENCE_TECHNOLOGY("과학기술"),
    TEACHING_PROFESSION("교직"),
    MILITARY_SCIENCE("군사학"),
    BASIC_SCIENCE_ENGINEERING("기초과학ㆍ공학"),
    SOCIAL_SCIENCE("사회"),
    ARTS_PHYSICAL_EDUCATION("예술체육"),
    FOREIGN_LANGUAGE("외국어"),
    HUMANITIES("인문"),
    GENERAL_ELECTIVE("일반선택"),
    MAJOR_FOUNDATION("전공기초"),
    MAJOR_ADVANCED("전공심화"),
    MAJOR_CORE("전공핵심"),
    ACADEMIC_FOUNDATION("학문의기초"),
    UNKNOWN("알 수 없는 값");

    private final String description;

    // api에서 돌어온 값은 enum으로 바꾸는 정적 팩토리 메서드
    public static ISU_FLD_NAME from(String value) {
        for (ISU_FLD_NAME isuFldName : values()) {
            if (isuFldName.description.equals(value)) {
                return isuFldName;
            }
        }

        return UNKNOWN;
    }
}
