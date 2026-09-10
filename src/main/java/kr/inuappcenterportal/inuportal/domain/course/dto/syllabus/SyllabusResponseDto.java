package kr.inuappcenterportal.inuportal.domain.course.dto.syllabus;

import kr.inuappcenterportal.inuportal.domain.course.dto.SyllabusContent;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.model.Syllabus;

public record SyllabusResponseDto(
        Long id,
        CourseOffering courseOffering,
        String subjectCode,
        String professor,
        SyllabusContent content
) {
    public static SyllabusResponseDto from(Syllabus syllabus) {
        return new SyllabusResponseDto(
                syllabus.getId(),
                syllabus.getCourseOffering(),
                syllabus.getSubjectCode(),
                syllabus.getProfessor(),
                syllabus.getContent()
        );
    }
}
