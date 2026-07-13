package kr.inuappcenterportal.inuportal.domain.course.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CompletionDivision {

    BASIC_SCIENCE("기초과학", "기과"),

    ESSENTIAL_GENERAL("교양필수", "교필"),
    BASIC_GENERAL("기초교양", "기교"),
    CORE_GENERAL("핵심교양", "핵교"),
    DEEPEN_GENERAL("심화교양", "심교"),
    SELECT_GENERAL("교양선택", "교선"),

    ESSENTIAL_MAJOR("전공필수", "전필"),
    BASIC_MAJOR("전공기초", "전기"),
    CORE_MAJOR("전공핵심", "전핵"),
    DEEPEN_MAJOR("전공심화", "전심"),
    SELECT_MAJOR("전공선택", "전선"),

    EDUCATION("교직", "교직"),
    MILITARY("군사학", "군사학"),
    SELECT_COMMON("일반선택", "일선"),

    UNKNOWN("알 수 없는 값", "알 수 없음");


    private final String description;
    private final String shortName;


    /**
     * 외부에서 들어온 문자열 값을 enum으로 바꾸는 정적 팩토리 메서드
     */
    public static CompletionDivision from(String value) {
        for (CompletionDivision completionDivision : values()) {
            if (completionDivision.description.equals(value)
                    || completionDivision.shortName.equals(value)) {
                return completionDivision;
            }
        }

        return UNKNOWN;
    }
}
