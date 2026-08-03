package kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum COLLEGE_NAME {
    BUSINESS_ADMINISTRATION("경영대학"),
    ENGINEERING("공과대학"),
    GENERAL_EDUCATION("교양"),
    TEACHING_PROFESSION("교직"),
    MILITARY_SCIENCE("군사학"),
    GLOBAL_POLITICS_ECONOMICS("글로벌정경대학"),
    ETC("기타"),
    NO_COLLEGE("단과대구분없음"),
    NO_COLLEGE_LAW("단과대구분없음(법학)"),
    URBAN_SCIENCES("도시과학대학"),
    EDUCATION("사범대학"),
    SOCIAL_SCIENCES("사회과학대학"),
    LIFE_SCIENCES_TECHNOLOGY("생명과학기술대학"),
    ARTS_PHYSICAL_EDUCATION("예술체육대학"),
    CONVERGENCE_LIBERAL_STUDIES("융합자유전공대학"),
    HUMANITIES("인문대학"),
    GENERAL_ELECTIVE("일선"),
    NATURAL_SCIENCES("자연과학대학"),
    INFORMATION_TECHNOLOGY("정보기술대학"),
    UNKNOWN("알 수 없는 값");


    private final String description;

    // api에서 돌어온 값은 enum으로 바꾸는 정적 팩토리 메서드
    public static COLLEGE_NAME from(String value) {
        for (COLLEGE_NAME collegeName : values()) {
            if (collegeName.description.equals(value)) {
                return collegeName;
            }
        }

        return UNKNOWN;
    }
}
