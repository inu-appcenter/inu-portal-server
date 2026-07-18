package kr.inuappcenterportal.inuportal.domain.customSchedule.dto;

import kr.inuappcenterportal.inuportal.domain.course.enums.DayOfWeek;

import java.time.LocalTime;

public record CustomScheduleMeetingCommand(
        String location,
        DayOfWeek day,
        LocalTime startTime,
        LocalTime endTime
) {
}
