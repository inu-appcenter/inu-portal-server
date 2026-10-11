package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SchedulePollCreateResponseDto(
        @Schema(description = "조율 ID", example = "812")
        Long pollId,
        @Schema(description = "참여 링크 토큰", example = "Q2x5bXh0b2dIc2VrVnJ0bW9p")
        String linkToken,
        @Schema(description = "참여 링크", example = "https://intip.inuappcenter.kr/schedule-polls/join/Q2x5bXh0b2dIc2VrVnJ0bW9p")
        String linkUrl,
        @Schema(description = "변경 번호", example = "1")
        long revision
) {
}
