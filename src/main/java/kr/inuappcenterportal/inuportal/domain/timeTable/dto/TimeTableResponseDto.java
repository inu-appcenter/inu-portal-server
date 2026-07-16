package kr.inuappcenterportal.inuportal.domain.timeTable.dto;

import kr.inuappcenterportal.inuportal.domain.timeTable.enums.Visibility;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;

public record TimeTableResponseDto(
        String timeTableName,
        boolean isPrimary,
        Visibility visibility
) {
    public static TimeTableResponseDto from(TimeTable timeTable) {
        return new TimeTableResponseDto(
                timeTable.getTimeTableName(),
                timeTable.isPrimary(),
                timeTable.getVisibility()
        );
    }
}
