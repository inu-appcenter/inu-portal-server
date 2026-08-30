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
    ETC("기타", "V000"),
    NO_COLLEGE("단과대구분없음", "0000465"),
    LAW_NO_COLLEGE("단과대구분없음(법학)", "0000706"),

    UNKNOWN("알 수 없는 값", null);


    private final String collegeName;
    private final String collegeCode;

    /**
     * 단과대코드로만 조회한다. 코드가 없거나(null/blank) 매칭되는 단과대가 없으면 null.
     * 학교 API 의 COLLEGE_CODE 는 이 enum 의 collegeCode 와 같은 체계다.
     */
    public static College fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        String trimmedCode = code.trim();
        for (College college : values()) {
            if (trimmedCode.equals(college.collegeCode)) {
                return college;
            }
        }

        return null;
    }

    /** 이 단과대에 해당하는 학교 API COLLEGE_CODE. (현재 1:1 이므로 대표 코드 하나) */
    public java.util.Set<String> apiCodes() {
        return collegeCode == null ? java.util.Set.of() : java.util.Set.of(collegeCode);
    }

    /**
     * 학교 API 응답 매핑용. 단과대코드 우선, 없으면 이름(enum 이름 / 한글 단과대명)으로 조회한다.
     * 어디에도 매칭되지 않으면 예외 대신 {@link #UNKNOWN} 을 반환한다.
     */
    public static College fromApi(String code, String name) {
        College byCode = fromCode(code);
        if (byCode != null) {
            return byCode;
        }

        if (name != null && !name.isBlank()) {
            String trimmedName = name.trim();
            for (College college : values()) {
                if (college.name().equalsIgnoreCase(trimmedName)
                        || college.collegeName.equals(trimmedName)
                        || Objects.equals(college.collegeCode, trimmedName)) {
                    return college;
                }
            }
        }

        return UNKNOWN;
    }

    /**
     * 이름/코드로 조회하되, 매칭되지 않으면 예외를 던진다.
     * (프론트에서 넘어온 필터/경로 값 검증처럼 "잘못된 값 = 400" 이어야 하는 곳에서 사용)
     */
    public static College from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        College college = fromApi(null, value);
        if (college == UNKNOWN) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }

        return college;
    }

}
