package kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ISU_NAME {
    TEACHING_PROFESSION("교직"),
    MILITARY_SCIENCE("군사학"),
    BASIC_LIBERAL_ARTS("기초교양"),
    ADVANCED_LIBERAL_ARTS("심화교양"),
    GENERAL_ELECTIVE("일반선택"),
    MAJOR_FOUNDATION("전공기초"),
    MAJOR_ADVANCED("전공심화"),
    MAJOR_CORE("전공핵심"),
    CORE_LIBERAL_ARTS("핵심교양"),
    UNKNOWN("알 수 없는 값");

    private final String description;

    // api에서 돌어온 값은 enum으로 바꾸는 정적 팩토리 메서드
    public static ISU_NAME from(String value) {
        for (ISU_NAME isuName : values()) {
            if (isuName.description.equals(value)) {
                return isuName;
            }
        }

        return UNKNOWN;
    }
}
