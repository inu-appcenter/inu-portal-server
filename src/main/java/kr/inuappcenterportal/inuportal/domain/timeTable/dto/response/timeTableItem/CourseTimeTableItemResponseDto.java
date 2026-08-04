package kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.Visibility;

import java.util.List;

public record CourseTimeTableItemResponseDto(
        @Schema(description = "강의 개설 id", example = "101")
        Long courseOfferingId,
        @Schema(description = "강의 id", example = "15")
        Long courseId,
        @Schema(description = "과목명", example = "웹프로그래밍")
        String title,
        @Schema(description = "교수명", example = "박기석")
        String professor,
        @Schema(description = "학수번호", example = "0001421001")
        String subjectNumber,
        @Schema(description = "학점", example = "3")
        Integer credit,
        @Schema(description = "강의 시간 목록")
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

    public static CourseTimeTableItemResponseDto of(
            CourseOffering courseOffering,
            List<CourseMeeting> meetings,
            Visibility visibility
    ) {
        boolean masked = visibility == Visibility.PROTECTED;

        Course course = courseOffering.getCourse();

        return new CourseTimeTableItemResponseDto(
                masked ? null : courseOffering.getId(),
                masked ? null : course.getId(),
                masked ? null : course.getTitle(),
                masked ? null : courseOffering.getProfessor(),
                masked ? null : courseOffering.getSubjectNumber(),
                masked ? null : course.getCredit(),
                meetings.stream()
                        .map(meeting -> TimeTableMeetingResponseDto.from(meeting, masked))
                        .toList()
        );
    }


}
