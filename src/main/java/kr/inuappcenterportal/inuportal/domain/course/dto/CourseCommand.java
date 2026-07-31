package kr.inuappcenterportal.inuportal.domain.course.dto;

import kr.inuappcenterportal.inuportal.domain.course.enums.CompletionDivision;
import kr.inuappcenterportal.inuportal.domain.course.enums.TargetGrade;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;

public record CourseCommand(
        Long courseId,
        String courseCode,
        String title,
        String englishTitle,
        Department department,
        TargetGrade targetGrade,
        CompletionDivision completionDivision,
        Integer credit
) {
}
