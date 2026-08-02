package kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CNCTR_ISU_NAME {

    NORMAL("일반(1~15주)"),
    INTENSIVE_A("집중A(1~8주)"),
    INTENSIVE_C("집중C(1~12주)");

    private final String description;
}
