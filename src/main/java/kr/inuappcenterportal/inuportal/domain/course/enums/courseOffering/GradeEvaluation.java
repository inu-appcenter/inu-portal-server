package kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering;

import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GradeEvaluation {
    RELATIVE("상대평가"),
    ABSOLUTE("절대평가"),
    PASS_RECOGNITION("이수인정");

    private final String description;

    public static GradeEvaluation from(String value) {
        for (GradeEvaluation gradeEvaluation : values()) {
            if (gradeEvaluation.description.equals(value))
                return gradeEvaluation;
        }

        throw new MyException(MyErrorCode.INVALID_INPUT);
    }
}
