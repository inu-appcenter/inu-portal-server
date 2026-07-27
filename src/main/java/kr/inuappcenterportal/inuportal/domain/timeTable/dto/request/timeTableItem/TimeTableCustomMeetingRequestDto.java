package kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTableItem;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kr.inuappcenterportal.inuportal.domain.course.enums.DayOfWeek;

import java.time.LocalTime;

public record TimeTableCustomMeetingRequestDto(
        @Schema(description = "장소", example = "07-504")
        String location,
        @Schema(description = "요일", example = "MONDAY")
        @NotNull DayOfWeek day,
        @Schema(description = "시작 시간", type = "string", example = "09:00", pattern = "HH:mm")
        @JsonFormat(pattern = "HH:mm")
        @NotNull LocalTime startTime,
        @Schema(description = "종료 시간", type = "string", example = "10:15", pattern = "HH:mm")
        @JsonFormat(pattern = "HH:mm")
        @NotNull LocalTime endTime
) {
}
