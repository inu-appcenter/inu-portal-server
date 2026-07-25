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
        String memo,
        @Schema(description = "시간표 요소 제목. COURSE는 강의명, CUSTOM은 커스텀 일정 제목입니다.", example = "알고리즘 스터디")
        String title
) {

    public static TimeTableItemResponseDto from(TimeTableItem timeTableItem) {
        return new TimeTableItemResponseDto(
                timeTableItem.getId(),
                timeTableItem.getType(),
                resolveTitle(timeTableItem),
                timeTableItem.getMemo()
        );
    }

    private static String resolveTitle(TimeTableItem timeTableItem) {
        return switch (timeTableItem.getType()) {
            case COURSE -> timeTableItem.getCourseOffering().getCourse().getTitle();
            case CUSTOM -> timeTableItem.getCustomSchedule().getTitle();
        };
    }
}
