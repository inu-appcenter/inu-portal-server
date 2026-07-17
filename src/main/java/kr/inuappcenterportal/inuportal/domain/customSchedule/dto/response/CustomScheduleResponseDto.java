package kr.inuappcenterportal.inuportal.domain.customSchedule.dto.response;

import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomSchedule;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomScheduleMeeting;

import java.util.List;

public record CustomScheduleResponseDto(
        Long id,
        String title,
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
