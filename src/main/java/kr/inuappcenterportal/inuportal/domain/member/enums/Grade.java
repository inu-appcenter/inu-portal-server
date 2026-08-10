package kr.inuappcenterportal.inuportal.domain.member.enums;

import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Grade {
    A_PLUS("A+"),
    A_ZERO("A0"),
    B_PLUS("B+"),
    B_ZERO("B0"),
    C_PLUS("C+"),
    C_ZERO("C0"),
    D_PLUS("D+"),
    D_ZERO("D0"),
    F("F"),
    P("P"),
    NP("NP");

    private final String value;

    public static Grade from(String value) {
        if (value == null || value.isBlank()) {
            return null; // 성적 미발표
        }

        for (Grade grade : values()) {
            if (grade.value.equals(value.trim())) {
                return grade;
            }
        }

        throw new MyException(MyErrorCode.INVALID_GRADE);
    }
}
