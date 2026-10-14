package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.DurationMode;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.ResultState;

import java.time.OffsetDateTime;
import java.util.List;

public record SchedulePollResultResponseDto(
        @Schema(description = "결과 번호", example = "3")
        int version,
        @Schema(description = "LATEST 최신 / STALE 이전 버전", example = "LATEST")
        ResultState state,
        @Schema(description = "최종 결과인지(조율 종료 시 고정)", example = "false")
        boolean isFinal,
        @Schema(description = "이 결과에 반영된 마지막 제출 시각", example = "2026-10-10T21:00:13+09:00")
        OffsetDateTime basedOnSubmittedAt,
        @Schema(description = "FIXED / AT_LEAST", example = "FIXED")
        DurationMode durationMode,
        @Schema(description = "회의 길이(분)", example = "60")
        int durationMin,
        @Schema(description = "참여자. index가 히트맵 숫자의 비트 위치")
        List<Participant> participants,
        @Schema(description = "열. heatmap[c]는 columns[c]")
        List<SchedulePollColumnResponseDto> columns,
        @Schema(description = "첫 칸 시각. heatmap[c][r]은 rowStart + 30분 × r 칸", example = "09:00")
        String rowStart,
        @Schema(description = "열당 칸 수", example = "18")
        int rowCount,
        @Schema(description = "heatmap[열][칸] = 그 칸에 가능한 참여자 비트마스크", example = "[[0, 0, 3, 7, 7, 7, 5, 1]]")
        int[][] heatmap,
        @Schema(description = "추천 후보(최대 3개)")
        List<Candidate> candidates
) {

    public record Participant(
            @Schema(description = "히트맵 비트 위치", example = "0")
            int index,
            @Schema(description = "회원 ID", example = "101")
            Long memberId,
            @Schema(description = "표시 이름(지금 이름)", example = "이지원")
            String displayName
    ) {
    }

    public record Candidate(
            @Schema(description = "순위 1~3", example = "1")
            int rank,
            @Schema(description = "열 키", example = "2026-10-12")
            String key,
            @Schema(description = "시작", example = "10:30")
            String start,
            @Schema(description = "끝", example = "11:30")
            String end,
            @Schema(description = "길이(분). FIXED면 회의 길이, AT_LEAST면 구간 길이", example = "60")
            int durationMin,
            @Schema(description = "가능한 참여자 비트마스크", example = "7")
            int availableMask,
            @Schema(description = "가능 인원", example = "3")
            int availableCount,
            @Schema(description = "전원 가능 여부", example = "true")
            boolean allAvailable
    ) {
    }
}
