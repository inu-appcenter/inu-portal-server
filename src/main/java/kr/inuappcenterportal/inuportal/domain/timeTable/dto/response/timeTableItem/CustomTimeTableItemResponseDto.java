package kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomSchedule;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomScheduleMeeting;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.Visibility;

import java.util.List;

public record CustomTimeTableItemResponseDto(
        @Schema(description = "커스텀 일정 id", example = "7")
        Long customScheduleId,
        @Schema(description = "커스텀 일정 제목", example = "알고리즘 스터디")
        String title,
        @Schema(description = "커스텀 일정 시간 목록")
        List<TimeTableMeetingResponseDto> meetings
) {
    public static CustomTimeTableItemResponseDto of(
            CustomSchedule customSchedule,
            List<CustomScheduleMeeting> meetings
    ) {
        return new CustomTimeTableItemResponseDto(
                customSchedule.getId(),
                customSchedule.getTitle(),
                meetings.stream()
                        .map(TimeTableMeetingResponseDto::from)
                        .toList()
        );
    }

    public static CustomTimeTableItemResponseDto of(
            CustomSchedule customSchedule,
            List<CustomScheduleMeeting> meetings,
            Visibility visibility
    ) {
        boolean masked = visibility == Visibility.PROTECTED;

        return new CustomTimeTableItemResponseDto(
                masked ? null : customSchedule.getId(),
                masked ? null : customSchedule.getTitle(),
                meetings.stream()
                        .map(meeting -> TimeTableMeetingResponseDto.from(meeting, masked))
                        .toList()
        );
    }
}
