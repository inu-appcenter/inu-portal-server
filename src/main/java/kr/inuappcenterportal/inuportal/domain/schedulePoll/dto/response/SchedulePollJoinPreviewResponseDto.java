package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.JoinBlockReason;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.ParticipantStatus;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.SchedulePollStatus;

import java.time.OffsetDateTime;

public record SchedulePollJoinPreviewResponseDto(
        @Schema(description = "조율 ID", example = "812")
        Long pollId,
        @Schema(description = "제목", example = "회의 시간 맞추기")
        String title,
        @Schema(description = "개설자 표시 이름", example = "이지원")
        String creatorDisplayName,
        @Schema(description = "상태. 마감 시각이 지났으면 CLOSED", example = "RESULT_READY")
        SchedulePollStatus status,
        @Schema(description = "참여자 수 (참고용)", example = "3")
        int participantCount,
        @Schema(description = "최대 참여자 수", example = "10")
        int maxParticipants,
        @Schema(description = "마감 시각", example = "2026-10-11T23:00:00+09:00")
        OffsetDateTime deadlineAt,
        @Schema(description = "입력 가능 여부 (참고용, 최종 판정은 제출 API)", example = "true")
        boolean canEnter,
        @Schema(description = "입력할 수 없는 이유. CLOSED / BLOCKED / FULL", example = "null")
        JoinBlockReason blockReason,
        @Schema(description = "SUBMITTED / null. 비로그인이면 항상 null", example = "null")
        ParticipantStatus myStatus
) {
}
