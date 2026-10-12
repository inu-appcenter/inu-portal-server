package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.ParticipantStatus;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.SchedulePollStatus;

import java.time.OffsetDateTime;

public record SchedulePollRevisionResponseDto(
        @Schema(description = "변경 번호", example = "14")
        long revision,
        @Schema(description = "상태. 마감 시각이 지났으면 CLOSED", example = "RESULT_READY")
        SchedulePollStatus status,
        @Schema(description = "결과 번호(1부터). 아직 결과가 없으면 null", example = "2", nullable = true)
        Integer resultVersion,
        @Schema(description = "참여자 수", example = "3")
        int participantCount,
        @Schema(description = "SUBMITTED / null", example = "SUBMITTED")
        ParticipantStatus myStatus,
        @Schema(description = "마감 시각", example = "2026-10-11T23:00:00+09:00")
        OffsetDateTime deadlineAt
) {
}
