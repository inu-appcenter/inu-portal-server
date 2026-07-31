package kr.inuappcenterportal.inuportal.domain.course.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Method {
    K_MOOC("K_MOOC"),
    RISE_TIMETABLE("RISE(시간표 있음)"),
    RISE("RISE(시간표 없음)"),
    E_LEARNING("e-learning"),
    E_LEARNING_HUSS("e-learning(HUSS)"),
    OFFLINE("강의(이론)"),
    SOCIAL_VOLUNTARY1("사회봉사(1)"),
    SOCIAL_VOLUNTARY2("사회봉사(2)"),
    SOCIAL_VOLUNTARY3("사회봉사(3)"),
    PAINTING("미술실기"),
    EXPERIMENT("실험실습"),
    OCU("열린사이버대학(OCU)"),
    ONLINE_BLENDED("온라인혼합형강좌"),
    ONLINE_BLENDED_HUSS("온라인혼합형강좌(HUSS)"),
    ART_PHYSICAL("예술체육실기"),
    THEORY("이론(어학)"),
    THEORY_EXPERIMENT("이론실험실습"),
    SELF("자기설계세미나"),
    EXERCISE("체육실기"),
    HUSS("현장형(HUSS)"),
    UNKNOWN("알 수 없음");

    private final String description;

    public static Method from(String value) {
        for (Method method : values()) {
            if (method.description.equals(value)) {
                return method;
            }
        }

        return UNKNOWN;
    }
}
