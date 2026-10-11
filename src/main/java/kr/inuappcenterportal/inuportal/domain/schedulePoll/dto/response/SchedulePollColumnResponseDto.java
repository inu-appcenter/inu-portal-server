package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// 그리드의 열. 정기면 key=MON, label=월 / 일회성이면 key=2026-10-12, label=10/12 (월)
public record SchedulePollColumnResponseDto(
        @Schema(description = "열 키", example = "2026-10-12")
        String key,
        @Schema(description = "표시 이름", example = "10/12 (월)")
        String label
) {
}
