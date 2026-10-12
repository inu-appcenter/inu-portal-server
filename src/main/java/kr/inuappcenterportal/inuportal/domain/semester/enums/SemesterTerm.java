package kr.inuappcenterportal.inuportal.domain.semester.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    @JsonCreator
    public static SemesterTerm from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String clean = value.trim().toUpperCase();
        for (SemesterTerm term : values()) {
            if (term.name().equalsIgnoreCase(clean) || term.displayName.equalsIgnoreCase(clean)) {
                return term;
            }
        }
        if (clean.equals("1") || clean.equals("1ST") || clean.equals("1학기") || clean.equals("10")) {
            return FIRST;
        }
        if (clean.equals("2") || clean.equals("2ND") || clean.equals("2학기") || clean.equals("20")) {
            return SECOND;
        }
        if (clean.contains("여름") || clean.equals("SUMMER") || clean.equals("30")) {
            return SUMMER;
        }
        if (clean.contains("겨울") || clean.equals("WINTER") || clean.equals("40")) {
            return WINTER;
        }
        throw new MyException(MyErrorCode.SEMESTER_NOT_FOUND);
    }

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
            return from(termCode);
        }
    }
}

