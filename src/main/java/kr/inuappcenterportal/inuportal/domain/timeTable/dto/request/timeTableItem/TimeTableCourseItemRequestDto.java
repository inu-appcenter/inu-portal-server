package kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTableItem;

import jakarta.validation.constraints.NotNull;

public record TimeTableCourseItemRequestDto(
        String memo,
        @NotNull Long courseOfferingId
) {
}
