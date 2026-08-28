package kr.inuappcenterportal.inuportal.domain.department.enums;

import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@Getter
@RequiredArgsConstructor
public enum College {

    COLLEGE_OF_HUMANITIES("인문대학", "A000"),
    COLLEGE_OF_NATURAL_SCIENCES("자연과학대학", "B000"),
    COLLEGE_OF_SOCIAL_SCIENCES("사회과학대학", "C000"),
    COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE("글로벌정경대학", "0000689"),
    COLLEGE_OF_ENGINEERING("공과대학", "E000"),
    COLLEGE_OF_INFORMATION_TECHNOLOGY("정보기술대학", "I000"),
    COLLEGE_OF_BUSINESS_ADMINISTRATION("경영대학", "J000"),
    COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION("예술체육대학", "0000190"),
    COLLEGE_OF_EDUCATION("사범대학", "0000063"),
    COLLEGE_OF_URBAN_SCIENCE("도시과학대학", "0000033"),
    COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY("생명과학기술대학", "0000182"),
    COLLEGE_OF_INTERDISCIPLINARY_STUDIES("융합자유전공대학", "0000837"),
    COLLEGE_OF_NULL("단과대 없음", null),

    GENERAL("교양", "X000"),
    GENERAL_ELECTIVE("일선", "W000"),
    TEACHING("교직", "Y000"),
    MILITARY_SCIENCE("군사학", "Z000"),
    HUSS("HUSS", "V000"),
    ETC("기타", "V000"),
    NO_COLLEGE("단과대구분없음", "0000465"),
    LAW_NO_COLLEGE("단과대구분없음(법학)", "0000706"),
    ;


    private final String collegeName;
    private final String collegeCode;

    public static College from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmedValue = value.trim();

        for (College college : values()) {
            if (college.name().equalsIgnoreCase(trimmedValue)
                    || college.collegeName.equals(trimmedValue)
                    || Objects.equals(college.collegeCode, trimmedValue)) {
                return college;
            }
        }

        throw new MyException(MyErrorCode.INVALID_INPUT);
    }

}
