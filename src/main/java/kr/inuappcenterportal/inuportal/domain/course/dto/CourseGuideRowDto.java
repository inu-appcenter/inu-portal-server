package kr.inuappcenterportal.inuportal.domain.course.dto;

import kr.inuappcenterportal.inuportal.domain.course.enums.CompletionDivision;
import kr.inuappcenterportal.inuportal.domain.course.enums.Method;
import kr.inuappcenterportal.inuportal.domain.course.enums.TargetGrade;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;

public record CourseGuideRowDto(
        Integer year,
        SemesterTerm semesterTerm,
        TargetGrade targetGrade,
        CompletionDivision completionDivision,
        String subjectNumber,
        String title,
        String credit,
        String professor,
        String meetingText,
        Method method,
        Department targetDepartment,
        String note
) {
}
