package kr.inuappcenterportal.inuportal.domain.customSchedule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.course.enums.DayOfWeek;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomScheduleMeeting;

import java.time.LocalTime;

@Schema(description = "커스텀일정 시간 응답 DTO")
public record CustomScheduleMeetingResponseDto(
        @Schema(description = "커스텀일정 시간 id", example = "1")
        Long id,

        @Schema(description = "커스텀일정 장소", example = "송도 스타벅스")
        String location,

        @Schema(description = "요일", example = "MONDAY")
        DayOfWeek day,

        @Schema(description = "시작 시간", example = "09:00")
        LocalTime startTime,

        @Schema(description = "종료 시간", example = "10:30")
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
