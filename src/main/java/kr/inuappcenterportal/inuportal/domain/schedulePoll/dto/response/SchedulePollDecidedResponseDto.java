package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

// 개설자가 정한 후보. 정한 때의 결과에서 열·시작·끝을 복사해 둔 값이다.
public record SchedulePollDecidedResponseDto(
        @Schema(description = "정한 결과 번호", example = "3")
        int resultVersion,
        @Schema(description = "정한 후보 순위 1~3", example = "1")
        int candidateRank,
        @Schema(description = "열 키. 정기면 요일(MON), 일회성이면 날짜(2026-10-12)", example = "2026-10-12")
        String key,
        @Schema(description = "시작", example = "10:30")
        String start,
        @Schema(description = "끝", example = "11:30")
        String end,
        @Schema(description = "정한 시각", example = "2026-10-11T21:05:00+09:00")
        OffsetDateTime decidedAt
) {
}
