package kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering;

import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.*;

public record CourseOfferingSearchCondition(
        Long semesterId,
        DEPT_NAME deptCode,
        COLLEGE_NAME collegeCode,
        HY_NAME hyCode,
        ISU_NAME isuCode,
        ISU_FLD_NAME isuFldCode,
        SSUP_TYPE_NAME ssupTypeCode,
        ENGLISH_NAME englishCode,
        Integer credit,
        String keyword
) {
}
