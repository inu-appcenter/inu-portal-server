package kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.TimeTableItemType;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTableItem;

public record TimeTableItemResponseDto(
        @Schema(description = "시간표 요소 id", example = "11")
        Long id,
        @Schema(description = "시간표 요소 타입", example = "CUSTOM")
        TimeTableItemType type,
        @Schema(description = "시간표 요소 메모", example = "스터디룸 예약")
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
