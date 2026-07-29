package kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering;

import kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting.CourseMeetingResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.enums.Language;
import kr.inuappcenterportal.inuportal.domain.course.enums.Method;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;

import java.util.List;

public record CourseOfferingResponseDto(
        String syllabus,
        String subjectNumber,
        Method method,
        String professor,
        Long courseId,
        String courseTitle,
        Long semesterId,
        Integer year,
        SemesterTerm term,
        Department targetDepartment,
        Language language,
        Integer capacity,
        Integer enrolledCount,
        String note,
        List<CourseMeetingResponseDto> meetings
) {

    public static CourseOfferingResponseDto from(CourseOffering courseOffering, List<CourseMeeting> meetings) {
        return new CourseOfferingResponseDto(
                courseOffering.getSyllabus(),
                courseOffering.getSubjectNumber(),
                courseOffering.getMethod(),
                courseOffering.getProfessor(),
                courseOffering.getCourse().getId(),
                courseOffering.getCourse().getTitle(),
                courseOffering.getSemester().getId(),
                courseOffering.getSemester().getYear(),
                courseOffering.getSemester().getTerm(),
                courseOffering.getTargetDepartment(),
                courseOffering.getLanguage(),
                courseOffering.getCapacity(),
                courseOffering.getEnrolledCount(),
                courseOffering.getNote(),
                meetings.stream().map(CourseMeetingResponseDto::from).toList()
        );
    }
}
