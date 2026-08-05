package kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting;

import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.DayOfWeek;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;

import java.time.LocalTime;

public record CourseMeetingResponseDto(
        Long id,
        String location,
        String sequence,
        DayOfWeek day,
        LocalTime startTime,
        LocalTime endTime
) {
    public static CourseMeetingResponseDto from(CourseMeeting meeting) {
        return new CourseMeetingResponseDto(
                meeting.getId(),
                meeting.getLocation(),
                meeting.getSequence(),
                meeting.getDay(),
                meeting.getStartTime(),
                meeting.getEndTime()
        );
    }
}
