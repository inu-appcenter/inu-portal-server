package kr.inuappcenterportal.inuportal.domain.customSchedule.dto.request;

import jakarta.validation.constraints.NotNull;
import kr.inuappcenterportal.inuportal.domain.course.enums.DayOfWeek;

import java.time.LocalTime;

public record CustomScheduleMeetingRequestDto(
        Long id,
        String location,
        @NotNull DayOfWeek day,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {
}
