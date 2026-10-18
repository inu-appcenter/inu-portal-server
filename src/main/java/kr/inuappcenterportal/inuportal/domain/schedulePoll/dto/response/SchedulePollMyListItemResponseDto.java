package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.ParticipantStatus;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.SchedulePollStatus;

import java.time.OffsetDateTime;

@Schema(description = "내 조율 목록의 한 줄")
public record SchedulePollMyListItemResponseDto(
        @Schema(description = "조율 ID", example = "812")
        Long pollId,
        @Schema(description = "제목", example = "회의 시간 맞추기")
        String title,
        @Schema(description = "상태. 마감 시각이 지났으면 CLOSED", example = "RESULT_READY")
        SchedulePollStatus status,
        @Schema(description = "내가 만든 조율인지", example = "true")
        boolean isHost,
        @Schema(description = "내 제출 상태. 제출하지 않았으면 null", example = "SUBMITTED", nullable = true)
        ParticipantStatus myStatus,
        @Schema(description = "참여자 수", example = "3")
        int participantCount,
        @Schema(description = "응답 마감. 없으면 null", example = "2026-10-11T23:00:00+09:00", nullable = true)
        OffsetDateTime deadlineAt,
        @Schema(description = "마지막으로 바뀐 시각", example = "2026-10-10T21:00:13+09:00")
        OffsetDateTime updatedAt
) {
}
