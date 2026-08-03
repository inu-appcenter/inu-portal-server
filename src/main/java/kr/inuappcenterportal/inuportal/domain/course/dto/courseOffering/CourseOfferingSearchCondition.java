package kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering;

import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.*;

public record CourseOfferingSearchCondition(
        Long semesterId,
        DEPT_NAME deptName,
        COLLEGE_NAME collegeName,
        HY_NAME hyName,
        ISU_NAME isuName,
        ISU_FLD_NAME isuFldName,
        SSUP_TYPE_NAME ssupTypeName,
        ENGLISH_NAME englishName,
        Integer credit,
        String keyword
) {
}
