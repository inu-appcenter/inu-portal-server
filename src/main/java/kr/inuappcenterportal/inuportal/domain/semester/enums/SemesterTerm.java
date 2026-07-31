package kr.inuappcenterportal.inuportal.domain.semester.enums;

import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SemesterTerm {
    FIRST("1학기"),
    SUMMER("여름계절학기"),
    SECOND("2학기"),
    WINTER("겨울계절학기");

    private final String displayName;

    public static SemesterTerm mapToTermCode(String termCode) {
        if (termCode.equals("10")) {
            return FIRST;
        } else if (termCode.equals("20")) {
            return SECOND;
        } else if (termCode.equals("30")) {
            return SUMMER;
        } else if (termCode.equals("40")) {
            return WINTER;
        } else {
            throw new MyException(MyErrorCode.SEMESTER_NOT_FOUND);
        }
    }
}
