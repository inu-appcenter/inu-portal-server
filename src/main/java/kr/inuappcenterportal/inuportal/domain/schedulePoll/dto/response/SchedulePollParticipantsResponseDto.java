package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

public record SchedulePollParticipantsResponseDto(
        @Schema(description = "참여자 수", example = "3")
        int participantCount,
        @Schema(description = "최대 참여자 수", example = "10")
        int maxParticipants,
        @Schema(description = "참여자 목록(참여 순). 제출하지 않은 개설자는 없다")
        List<Participant> participants
) {

    public record Participant(
            @Schema(description = "회원 ID", example = "101")
            Long memberId,
            @Schema(description = "표시 이름", example = "이지원")
            String displayName,
            @Schema(description = "가린 학번", example = "2024***01")
            String maskedStudentId,
            @Schema(description = "개설자인지", example = "true")
            boolean isHost,
            @Schema(description = "참여(첫 제출) 시각", example = "2026-10-10T20:11:02+09:00")
            OffsetDateTime joinedAt,
            @Schema(description = "마지막 제출 시각", example = "2026-10-10T20:15:40+09:00")
            OffsetDateTime submittedAt
    ) {
    }
}
