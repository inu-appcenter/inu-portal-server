package kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering;

import kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting.CourseMeetingResponseDto;
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
        String courseCode,
        String courseTitle,
        String courseTitleEng,

        Long semesterId,
        Integer year,
        SemesterTerm term,
        String termName,

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

        String hyCode,
        String hyName,

        String englishCode,
        String englishName,

        Integer credit,

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
                courseOffering.getCourse().getCourseCode(),
                courseOffering.getCourse().getTitle(),
                courseOffering.getCourse().getEnglishTitle(),
                courseOffering.getSemester().getId(),
                courseOffering.getSemester().getYear(),
                courseOffering.getSemester().getTerm(),
                courseOffering.getSemester().getTerm().getDisplayName(),
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
                courseOffering.getHyName().name(),
                courseOffering.getHyName().getDescription(),
                courseOffering.getEnglishName().name(),
                courseOffering.getEnglishName().getDescription(),
                courseOffering.getCredit(),
                courseOffering.getCapacity(),
                courseOffering.getEnrolledCount(),
                courseOffering.getNote(),
                meetings.stream().map(CourseMeetingResponseDto::from).toList()
        );
    }
}
