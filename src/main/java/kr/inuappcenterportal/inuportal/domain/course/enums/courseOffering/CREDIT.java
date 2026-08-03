package kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CREDIT {
    GRADE_1("1"),
    GRADE_2("2"),
    GRADE_3("3"),
    GRADE_4("4"),
    GRADE_5("5"),
    GRADE_6("6"),
    GRADE_12("12"),
    GRADE_15("15"),
    UNKNOWN("알 수 없는 값");

    private final String description;

    // api에서 돌어온 값은 enum으로 바꾸는 정적 팩토리 메서드
    public static CREDIT from(String value) {
        for (CREDIT credit : values()) {
            if (credit.description.equals(value)) {
                return credit;
            }
        }

        return UNKNOWN;
    }
}
