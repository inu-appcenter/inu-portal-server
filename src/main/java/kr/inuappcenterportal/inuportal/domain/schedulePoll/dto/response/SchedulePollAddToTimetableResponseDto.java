package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SchedulePollAddToTimetableResponseDto(
        @Schema(description = "만든(또는 이미 있던) 커스텀 일정 ID", example = "7712")
        Long customScheduleId,
        @Schema(description = "이미 추가했던 결과인지", example = "false")
        boolean alreadyAdded
) {
}
