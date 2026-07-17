package kr.inuappcenterportal.inuportal.domain.customSchedule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomSchedule;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomScheduleMeeting;

import java.util.List;

@Schema(description = "커스텀일정 응답 DTO")
public record CustomScheduleResponseDto(
        @Schema(description = "커스텀일정 id", example = "1")
        Long id,

        @Schema(description = "커스텀일정 제목", example = "알바")
        String title,

        @Schema(description = "커스텀일정 시간 목록")
        List<CustomScheduleMeetingResponseDto> meetings
) {

    public static CustomScheduleResponseDto from(
            CustomSchedule customSchedule,
            List<CustomScheduleMeeting> meetings) {
        return new CustomScheduleResponseDto(
                customSchedule.getId(),
                customSchedule.getTitle(),
                meetings.stream()
                        .map(CustomScheduleMeetingResponseDto::from)
                        .toList()
        );
    }

    public static CustomScheduleResponseDto from(CustomSchedule customSchedule) {
        return new CustomScheduleResponseDto(
                customSchedule.getId(),
                customSchedule.getTitle(),
                null
        );
    }
}
