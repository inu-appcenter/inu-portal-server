package kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem;

import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomSchedule;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomScheduleMeeting;

import java.util.List;

public record CustomTimeTableItemResponseDto(
        Long customScheduleId,
        String title,
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
}

