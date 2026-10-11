package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.AvailableRangeDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.ParticipantStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record SchedulePollMySubmissionResponseDto(
        @Schema(description = "SUBMITTED", example = "SUBMITTED")
        ParticipantStatus status,
        @Schema(description = "내 제출 번호. 재제출할 때 baseSubmissionRevision으로 보낸다", example = "3")
        int submissionRevision,
        @Schema(description = "가능 시간 구간 목록")
        List<AvailableRangeDto> available,
        @Schema(description = "시간표에서 불러온 수업 ID", example = "[4412, 4413]")
        List<Long> importedCourseOfferingIds,
        @Schema(description = "마지막 제출 시각", example = "2026-10-10T21:04:11+09:00")
        OffsetDateTime submittedAt
) {
}
