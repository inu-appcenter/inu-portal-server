package kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem;

import kr.inuappcenterportal.inuportal.domain.timeTable.enums.TimeTableItemType;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTableItem;

public record TimeTableItemResponseDto(
        Long id,
        TimeTableItemType type,
        String memo
) {

    public static TimeTableItemResponseDto from(TimeTableItem timeTableItem) {
        return new TimeTableItemResponseDto(
                timeTableItem.getId(),
                timeTableItem.getType(),
                timeTableItem.getMemo()
        );
    }
}
