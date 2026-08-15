package kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTableItem;

import io.swagger.v3.oas.annotations.media.Schema;

public record TimeTableItemMemoUpdateRequestDto(
        @Schema(description = "시간표 요소 메모. null이면 메모를 삭제합니다.", example = "스터디룸 예약", nullable = true)
        String memo
) {
}
