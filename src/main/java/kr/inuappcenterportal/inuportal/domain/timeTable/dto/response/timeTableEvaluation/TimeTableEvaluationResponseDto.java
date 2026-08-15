package kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableEvaluation;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTableEvaluation;

import java.time.LocalDateTime;

public record TimeTableEvaluationResponseDto(
        @Schema(description = "시간표 id", example = "1")
        Long timeTableId,
        @Schema(description = "AI 평가 내용 전문 (Markdown 포맷)", example = "🔥 **월요병과 우주공강의 환상적인 콜라보!**\n\n저런... 수요일에 3시간 우주공강이 있네요...")
        String content,
        @Schema(description = "시간표 상태 해시", example = "a1b2c3d4...")
        String timetableHash,
        @Schema(description = "캐시된 결과 여부 (true: 기존 캐시, false: 신규 생성)", example = "true")
        boolean isCached,
        @Schema(description = "동일 시간표 상태에서의 다시 생성 횟수 (최대 3회)", example = "1")
        int regenerateCount,
        @Schema(description = "남은 다시 생성 가능 횟수", example = "2")
        int remainingCount,
        @Schema(description = "평가 생성/수정 일시")
        LocalDateTime updatedAt
) {
    public static final int MAX_REGENERATE_COUNT = 3;

    public static TimeTableEvaluationResponseDto of(TimeTableEvaluation evaluation, boolean isCached) {
        int regCount = evaluation.getRegenerateCount();
        int remaining = Math.max(0, MAX_REGENERATE_COUNT - regCount);
        return new TimeTableEvaluationResponseDto(
                evaluation.getTimeTable().getId(),
                evaluation.getContent(),
                evaluation.getTimetableHash(),
                isCached,
                regCount,
                remaining,
                evaluation.getModifiedDate() != null ? evaluation.getModifiedDate() : evaluation.getCreateDate()
        );
    }
}
