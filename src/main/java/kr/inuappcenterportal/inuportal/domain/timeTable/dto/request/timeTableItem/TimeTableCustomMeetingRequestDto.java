package kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTableItem;

import jakarta.validation.constraints.NotNull;
import kr.inuappcenterportal.inuportal.domain.course.enums.DayOfWeek;

import java.time.LocalTime;

public record TimeTableCustomMeetingRequestDto(
        String location,
        @NotNull DayOfWeek day,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {
}
