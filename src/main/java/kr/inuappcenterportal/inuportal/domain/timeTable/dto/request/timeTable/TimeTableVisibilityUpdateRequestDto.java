package kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.Visibility;

public record TimeTableVisibilityUpdateRequestDto(
        @Schema(description = "공개범위", example = "PUBLIC")
        @NotNull
        Visibility visibility
) {
}
