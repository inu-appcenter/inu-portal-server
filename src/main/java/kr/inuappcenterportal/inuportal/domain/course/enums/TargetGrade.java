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
    UNKNOWN("알 수 없음");

    private final String displayName;

    /**
     * 외부에서 들어온 문자열 값을 enum으로 바꾸는 정적 팩토리 메서드
     */
    public static TargetGrade from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }

        return switch (value.trim()) {
            case "1", "1학년" -> FIRST;
            case "2", "2학년" -> FIRST;
            case "3", "3학년" -> FIRST;
            case "4", "4학년" -> FIRST;
            case "공통" -> COMMON;
            default -> UNKNOWN;
        };
    }
}
