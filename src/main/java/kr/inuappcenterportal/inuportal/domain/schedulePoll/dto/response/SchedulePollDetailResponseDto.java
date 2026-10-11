package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.DurationMode;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.ParticipantStatus;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.SchedulePollStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record SchedulePollDetailResponseDto(
        @Schema(description = "조율 ID", example = "812")
        Long pollId,
        @Schema(description = "제목", example = "회의 시간 맞추기")
        String title,
        @Schema(description = "상태. 마감 시각이 지났으면 CLOSED", example = "RESULT_READY")
        SchedulePollStatus status,
        @Schema(description = "정기 여부", example = "false")
        boolean isRecurring,
        @Schema(description = "그리드 열")
        List<SchedulePollColumnResponseDto> columns,
        @Schema(description = "시간대 시작", example = "09:00")
        String rangeStart,
        @Schema(description = "시간대 끝", example = "18:00")
        String rangeEnd,
        @Schema(description = "FIXED / AT_LEAST", example = "FIXED")
        DurationMode durationMode,
        @Schema(description = "회의 길이(분)", example = "60")
        int durationMin,
        @Schema(description = "마감 시각", example = "2026-10-11T23:00:00+09:00")
        OffsetDateTime deadlineAt,
        @Schema(description = "개설자")
        Creator creator,
        @Schema(description = "내가 개설자인지", example = "true")
        boolean isHost,
        @Schema(description = "SUBMITTED / null(제출한 적 없음)", example = "SUBMITTED")
        ParticipantStatus myStatus,
        @Schema(description = "참여자 수", example = "3")
        int participantCount,
        @Schema(description = "최대 참여자 수", example = "10")
        int maxParticipants,
        @Schema(description = "참여 링크", example = "https://intip.inuappcenter.kr/schedule-polls/join/Q2x5bXh0b2dIc2VrVnJ0bW9p")
        String linkUrl,
        @Schema(description = "변경 번호", example = "14")
        long revision,
        @Schema(description = "결과 번호", example = "2")
        int resultVersion,
        @Schema(description = "끝난 시각. 마감이 지났는데 아직 처리 전이면 마감 시각", example = "null")
        OffsetDateTime closedAt
) {

    public record Creator(
            @Schema(description = "회원 ID", example = "101")
            Long memberId,
            @Schema(description = "표시 이름", example = "이지원")
            String displayName
    ) {
    }
}
