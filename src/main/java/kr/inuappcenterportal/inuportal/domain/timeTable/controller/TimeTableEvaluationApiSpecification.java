package kr.inuappcenterportal.inuportal.domain.timeTable.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableEvaluation.TimeTableEvaluationResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "TimeTable Evaluation (AI 시간표 평가)", description = "AI 횃불이의 시간표 평가 및 코칭 API")
public interface TimeTableEvaluationApiSpecification {

    @Operation(summary = "시간표 AI 평가 캐시 조회", description = "기존에 평가받은 시간표 AI 분석 결과를 조회합니다. 시간표가 수정되었거나 평가가 없으면 data가 null로 반환됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "캐시 조회 성공"),
            @ApiResponse(responseCode = "404", description = "시간표를 찾을 수 없음")
    })
    ResponseEntity<ResponseDto<TimeTableEvaluationResponseDto>> getCachedEvaluation(
            @Parameter(hidden = true) @AuthenticationPrincipal Member member,
            @Parameter(description = "시간표 ID", example = "1") @PathVariable Long timeTableId
    );

    @Operation(summary = "시간표 AI 평가 실시간 SSE 스트리밍", description = "vLLM 기반으로 시간표를 AI가 실시간 평가하고 SSE(Server-Sent Events)로 스트리밍합니다. 평가 완료 시 DB에 자동 캐시됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE 스트림 연결 성공", content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "시간표에 등록된 강의/일정이 없음"),
            @ApiResponse(responseCode = "404", description = "시간표를 찾을 수 없음")
    })
    SseEmitter streamEvaluation(
            @Parameter(hidden = true) @AuthenticationPrincipal Member member,
            @Parameter(description = "시간표 ID", example = "1") @PathVariable Long timeTableId,
            @Parameter(description = "강제 재평가 여부 (기존 캐시 무시)", example = "false") @RequestParam(defaultValue = "false") boolean forceRefresh
    );
}
