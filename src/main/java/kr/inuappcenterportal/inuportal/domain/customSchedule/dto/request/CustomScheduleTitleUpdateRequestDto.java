package kr.inuappcenterportal.inuportal.domain.customSchedule.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CustomScheduleTitleUpdateRequestDto(
        @NotBlank String title
) {
}
