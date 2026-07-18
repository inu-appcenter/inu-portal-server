package kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTable;

import jakarta.validation.constraints.NotBlank;

public record TimeTableCreateRequestDto(
        @NotBlank
        String timeTableName
) {
}
