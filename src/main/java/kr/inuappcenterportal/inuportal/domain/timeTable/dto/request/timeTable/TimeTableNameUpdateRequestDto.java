package kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TimeTableNameUpdateRequestDto(
        @Schema(description = "시간표 이름", example = "2026-1학기")
        @NotBlank
        String timeTableName
) {
}
