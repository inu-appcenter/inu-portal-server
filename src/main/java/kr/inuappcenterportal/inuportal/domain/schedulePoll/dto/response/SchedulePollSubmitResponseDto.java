package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.ParticipantStatus;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.SchedulePollStatus;

public record SchedulePollSubmitResponseDto(
        @Schema(description = "SUBMITTED", example = "SUBMITTED")
        ParticipantStatus myStatus,
        @Schema(description = "이번이 첫 제출인지", example = "false")
        boolean isFirstSubmission,
        @Schema(description = "내 제출 번호", example = "4")
        int submissionRevision,
        @Schema(description = "조율 상태", example = "RESULT_READY")
        SchedulePollStatus status,
        @Schema(description = "참여자 수", example = "3")
        int participantCount,
        @Schema(description = "결과 번호. 참여자 2명 미만이면 null", example = "3")
        Integer resultVersion,
        @Schema(description = "변경 번호", example = "15")
        long revision
) {
}
