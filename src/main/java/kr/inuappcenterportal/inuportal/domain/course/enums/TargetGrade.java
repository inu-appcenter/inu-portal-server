package kr.inuappcenterportal.inuportal.domain.course.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TargetGrade {
    COMMON("공통"),
    FIRST("1학년"),
    SECOND("2학년"),
    THIRD("3학년"),
    FOURTH("4학년"),
    UNKNOWN("미정");

    private final String displayName;

    /**
     * 외부에서 들어온 문자열 값을 enum으로 바꾸는 정적 팩토리 메서드
     */
    public static TargetGrade from(String value) {
        for (TargetGrade targetGrade : values()) {
            if (targetGrade.displayName.equals(value)) {
                return targetGrade;
            }
        }

        return UNKNOWN;
    }
}
