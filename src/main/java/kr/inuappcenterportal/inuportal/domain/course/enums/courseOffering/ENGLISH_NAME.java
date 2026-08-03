package kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ENGLISH_NAME {
    GLOBAL_ES("글로벌강의(ES)"),
    UN_TARGET("비대상"),
    ENGLISH_EN("원어강의(EN)"),
    UNKNOWN("알 수 없는 값");

    private final String description;

    // api에서 돌어온 값은 enum으로 바꾸는 정적 팩토리 메서드
    public static ENGLISH_NAME from(String value) {
        for (ENGLISH_NAME englishName : values()) {
            if (englishName.description.equals(value)) {
                return englishName;
            }
        }

        return UNKNOWN;
    }
}
