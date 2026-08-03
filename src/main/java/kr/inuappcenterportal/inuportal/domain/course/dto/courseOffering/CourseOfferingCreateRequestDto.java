package kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering;

import kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting.CourseMeetingRequestDto;
import kr.inuappcenterportal.inuportal.domain.course.enums.course.Language;
import kr.inuappcenterportal.inuportal.domain.course.enums.course.Method;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;

import java.util.List;

public record CourseOfferingCreateRequestDto(
        Long courseId,
        String courseTitle,
        Department department,
        Long semesterId,
        String subjectNumber,
        String professor,
        Method method,
        Language language,
        Department targetDepartment,
        Integer capacity,
        Integer enrolledCount,
        String syllabus,
        String note,
        List<CourseMeetingRequestDto> meetings
) {
}
