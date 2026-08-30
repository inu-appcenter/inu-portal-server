package kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering;

import kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting.CourseOfferingMeetingFilter;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.CourseOfferingSort;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.MeetingFilterMode;
import kr.inuappcenterportal.inuportal.domain.department.enums.College;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;

import java.util.List;

public record CourseOfferingSearchCondition(
        Long semesterId,
        Department deptName,
        College collegeName,
        List<String> hyNames,
        List<String> isuNames,
        List<String> isuFldNames,
        List<String> ssupTypeNames,
        List<Integer> credits,
        String keyword,
        MeetingFilterMode filterMode,
        List<CourseOfferingMeetingFilter> meetings,
        CourseOfferingSort sort
) {
}
