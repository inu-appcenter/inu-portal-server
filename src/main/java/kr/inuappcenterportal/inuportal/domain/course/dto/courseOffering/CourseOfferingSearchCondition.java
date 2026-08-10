package kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering;

import kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting.CourseOfferingMeetingFilter;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.*;

import java.util.List;

public record CourseOfferingSearchCondition(
        Long semesterId,
        DEPT_NAME deptName,
        COLLEGE_NAME collegeName,
        List<HY_NAME> hyNames,
        List<ISU_NAME> isuNames,
        List<ISU_FLD_NAME> isuFldNames,
        List<SSUP_TYPE_NAME> ssupTypeNames,
        List<Integer> credits,
        String keyword,
        MeetingFilterMode filterMode,
        List<CourseOfferingMeetingFilter> meetings,
        CourseOfferingSort sort
) {
}
