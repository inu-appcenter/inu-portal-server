package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SchedulePollAddToTimetableRequestDto(
        @Schema(description = "추가할 후보 순위 1~3", example = "1")
        @NotNull
        @Min(1)
        @Max(3)
        Integer candidateRank
) {
}
