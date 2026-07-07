package kr.inuappcenterportal.inuportal.domain.department.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum College {

    COLLEGE_OF_HUMANITIES("인문대학"),
    COLLEGE_OF_NATURAL_SCIENCES("자연과학대학"),
    COLLEGE_OF_SOCIAL_SCIENCES("사회과학대학"),
    COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE("글로벌정경대학"),
    COLLEGE_OF_ENGINEERING("공과대학"),
    COLLEGE_OF_INFORMATION_TECHNOLOGY("정보기술대학"),
    COLLEGE_OF_BUSINESS_ADMINISTRATION("경영대학"),
    COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION("예술체육대학"),
    COLLEGE_OF_EDUCATION("사범대학"),
    COLLEGE_OF_URBAN_SCIENCE("도시과학대학"),
    COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY("생명과학기술대학"),
    COLLEGE_OF_INTERDISCIPLINARY_STUDIES("융합자유전공대학"),
    COLLEGE_OF_NULL("단과대 없음"),
    GENERAL("교양");

    private final String collegeName;
}
