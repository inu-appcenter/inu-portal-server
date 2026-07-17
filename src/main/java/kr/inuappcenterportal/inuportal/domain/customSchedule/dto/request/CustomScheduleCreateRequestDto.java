package kr.inuappcenterportal.inuportal.domain.customSchedule.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CustomScheduleCreateRequestDto(
        @NotBlank String title,
        @NotEmpty List<@Valid CustomScheduleMeetingRequestDto> meetings
) {
}
