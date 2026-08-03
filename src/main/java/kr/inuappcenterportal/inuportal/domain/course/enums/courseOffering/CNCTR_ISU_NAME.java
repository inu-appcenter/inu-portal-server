package kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CNCTR_ISU_NAME {

    NORMAL("일반(1~15주)"),
    INTENSIVE_A("집중A(1~8주)"),
    INTENSIVE_C("집중C(1~12주)"),
    UNKNOWN("알 수 없는 값");


    private final String description;

    // api에서 돌어온 값은 enum으로 바꾸는 정적 팩토리 메서드
    public static CNCTR_ISU_NAME from(String value) {
        for (CNCTR_ISU_NAME cnctrIsuName : values()) {
            if (cnctrIsuName.description.equals(value)) {
                return cnctrIsuName;
            }
        }

        return UNKNOWN;
    }
}
