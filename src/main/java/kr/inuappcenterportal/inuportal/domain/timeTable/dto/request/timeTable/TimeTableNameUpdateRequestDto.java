package kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTable;

import jakarta.validation.constraints.NotBlank;

public record TimeTableNameUpdateRequestDto(
        @NotBlank
        String timeTableName
) {
}
