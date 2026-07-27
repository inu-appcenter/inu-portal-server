package kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTableItem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record TimeTableCourseItemRequestDto(
        @Schema(description = "시간표 요소 메모", example = "중간고사 4/22, 기말고사 6/17")
        String memo,
        @Schema(description = "시간표에 추가할 개설 강의 id", example = "101")
        @NotNull Long courseOfferingId
) {
}
