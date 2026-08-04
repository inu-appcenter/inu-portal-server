package kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HY_NAME {
    GRADE1("1"),
    GRADE2("2"),
    GRADE3("3"),
    GRADE4("4"),
    ALL("전학년"),
    UNKNOWN("알 수 없는 값");

    private final String description;

    // api에서 돌어온 값은 enum으로 바꾸는 정적 팩토리 메서드
    public static HY_NAME from(String value) {
        for (HY_NAME hyName : values()) {
            if (hyName.description.equals(value)) {
                return hyName;
            }
        }

        return UNKNOWN;
    }
}
