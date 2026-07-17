package kr.inuappcenterportal.inuportal.domain.customSchedule.dto.response;

import kr.inuappcenterportal.inuportal.domain.course.enums.DayOfWeek;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomScheduleMeeting;

import java.time.LocalTime;

public record CustomScheduleMeetingResponseDto(
        Long id,
        String location,
        DayOfWeek day,
        LocalTime startTime,
        LocalTime endTime
) {
    public static CustomScheduleMeetingResponseDto from(CustomScheduleMeeting meeting) {
        return new CustomScheduleMeetingResponseDto(
                meeting.getId(),
                meeting.getLocation(),
                meeting.getDay(),
                meeting.getStartTime(),
                meeting.getEndTime()
        );
    }
}
