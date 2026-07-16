package kr.inuappcenterportal.inuportal.domain.timeTable.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TimeTableNameUpdateRequestDto(
        @NotBlank
        String timeTableName
) {
}
