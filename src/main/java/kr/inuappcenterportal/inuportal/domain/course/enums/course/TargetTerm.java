package kr.inuappcenterportal.inuportal.domain.course.enums.course;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TargetTerm {
    // 교육과정에서 파싱한 대상 학기 관련 enum
    FIRST("1학기"),
    SECOND("2학기"),
    COMMON("공통"),
    UNKNOWN("알 수 없음");

    private final String displayName;


    /**
     * 외부에서 들어온 문자열 값을 enum으로 바꾸는 정적 팩토리 메서드
     */
    public static TargetTerm from(String value) {
        for (TargetTerm targetTerm : values()) {
            if (targetTerm.displayName.equals(value)) {
                return targetTerm;
            }
        }

        return UNKNOWN;
    }
}
