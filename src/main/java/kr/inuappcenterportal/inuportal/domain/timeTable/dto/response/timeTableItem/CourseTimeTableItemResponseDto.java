package kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem;

import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;

import java.util.List;

public record CourseTimeTableItemResponseDto(
        Long courseOfferingId,
        Long courseId,
        String title,
        String professor,
        String subjectNumber,
        String credit,
        List<TimeTableMeetingResponseDto> meetings
) {
    public static CourseTimeTableItemResponseDto of(
            CourseOffering courseOffering,
            List<CourseMeeting> meetings
    ) {
        Course course = courseOffering.getCourse();

        return new CourseTimeTableItemResponseDto(
                courseOffering.getId(),
                course.getId(),
                course.getTitle(),
                courseOffering.getProfessor(),
                courseOffering.getSubjectNumber(),
                course.getCredit(),
                meetings.stream()
                        .map(TimeTableMeetingResponseDto::from)
                        .toList()
        );
    }

}
