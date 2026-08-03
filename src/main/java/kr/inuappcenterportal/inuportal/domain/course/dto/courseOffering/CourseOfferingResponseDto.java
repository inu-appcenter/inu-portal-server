package kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering;

import kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting.CourseMeetingResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.enums.Language;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;

import java.util.List;

public record CourseOfferingResponseDto(
        Long id,
        String syllabus,
        String subjectNumber,
        String professor,

        Long courseId,
        String courseTitle,

        Long semesterId,
        Integer year,
        SemesterTerm term,

        String cnctrIsuCode,
        String cnctrIsuName,

        String deptCode,
        String deptName,

        String collegeCode,
        String collegeName,

        String isuFldCode,
        String isuFldName,

        String isuCode,
        String isuName,

        String ssupTypeCode,
        String ssupTypeName,

        String creditCode,
        String creditName,

        Language language,
        Integer capacity,
        Integer enrolledCount,
        String note,
        List<CourseMeetingResponseDto> meetings
) {

    public static CourseOfferingResponseDto from(CourseOffering courseOffering, List<CourseMeeting> meetings) {
        return new CourseOfferingResponseDto(
                courseOffering.getId(),
                courseOffering.getSyllabus(),
                courseOffering.getSubjectNumber(),
                courseOffering.getProfessor(),
                courseOffering.getCourse().getId(),
                courseOffering.getCourse().getTitle(),
                courseOffering.getSemester().getId(),
                courseOffering.getSemester().getYear(),
                courseOffering.getSemester().getTerm(),
                courseOffering.getCnctrIsuName().name(),
                courseOffering.getCnctrIsuName().getDescription(),
                courseOffering.getDeptName().name(),
                courseOffering.getDeptName().getDescription(),
                courseOffering.getCollegeName().name(),
                courseOffering.getCollegeName().getDescription(),
                courseOffering.getIsuFldName().name(),
                courseOffering.getIsuFldName().getDescription(),
                courseOffering.getIsuName().name(),
                courseOffering.getIsuName().getDescription(),
                courseOffering.getSsupTypeName().name(),
                courseOffering.getSsupTypeName().getDescription(),
                courseOffering.getCredit().name(),
                courseOffering.getCredit().getDescription(),
                courseOffering.getLanguage(),
                courseOffering.getCapacity(),
                courseOffering.getEnrolledCount(),
                courseOffering.getNote(),
                meetings.stream().map(CourseMeetingResponseDto::from).toList()
        );
    }
}
