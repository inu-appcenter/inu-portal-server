package kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SSUP_TYPE_NAME {

    K_MOOC("K-MOOC"),
    RISE_WITHOUT_TIMETABLE("RISE(시간표 없음)"),
    RISE_WITH_TIMETABLE("RISE(시간표 있음)"),
    E_LEARNING("e-Learning"),
    E_LEARNING_HUSS("e-Learning(HUSS)"),
    THEORY_LECTURE("강의(이론)"),
    BEYOND_WALL_VOLUNTEER_1("담장너머~,사회봉사(1)"),
    ART_PRACTICE("미술실기"),
    VOLUNTEER_2("사회봉사(2)"),
    VOLUNTEER_3("사회봉사(3)"),
    EXPERIMENT_PRACTICE("실험실습"),
    OCU("열린사이버대학(OCU)"),
    ARTS_AND_PHYSICAL_PRACTICE("예술체육실기"),
    BLENDED_ONLINE_COURSE("온라인혼합형강좌"),
    BLENDED_ONLINE_COURSE_HUSS("온라인혼합형강좌(HUSS)"),
    LANGUAGE_THEORY("이론(어학)"),
    THEORY_AND_EXPERIMENT_PRACTICE("이론실험실습"),
    SELF_DESIGNED_SEMINAR("자기설계세미나"),
    PHYSICAL_PRACTICE("체육실기"),
    OFFLINE_HUSS("현장형(HUSS)"),
    UNKNOWN("알 수 없는 값");

    private final String description;

    // api에서 돌어온 값은 enum으로 바꾸는 정적 팩토리 메서드
    public static SSUP_TYPE_NAME from(String value) {
        for (SSUP_TYPE_NAME ssupTypeName : values()) {
            if (ssupTypeName.description.equals(value)) {
                return ssupTypeName;
            }
        }

        return UNKNOWN;
    }
}
