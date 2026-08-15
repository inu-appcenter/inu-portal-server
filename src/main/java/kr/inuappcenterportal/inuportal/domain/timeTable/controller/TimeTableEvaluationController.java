package kr.inuappcenterportal.inuportal.domain.timeTable.controller;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableEvaluation.TimeTableEvaluationResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.service.TimeTableEvaluationService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timetables")
public class TimeTableEvaluationController implements TimeTableEvaluationApiSpecification {

    private final TimeTableEvaluationService timeTableEvaluationService;

    /**
     * 시간표 AI 평가 캐시 조회
     */
    @GetMapping("/{timeTableId}/evaluation")
    public ResponseEntity<ResponseDto<TimeTableEvaluationResponseDto>> getCachedEvaluation(
            @AuthenticationPrincipal Member member,
            @PathVariable Long timeTableId
    ) {
        TimeTableEvaluationResponseDto response =
                timeTableEvaluationService.getCachedEvaluation(member.getId(), timeTableId);

        return ResponseEntity.ok(
                ResponseDto.of(response, response != null ? "시간표 평가 캐시 조회 성공" : "저장된 평가가 없습니다.")
        );
    }

    /**
     * 시간표 AI 평가 SSE 스트리밍
     */
    @GetMapping(value = "/{timeTableId}/evaluation/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvaluation(
            @AuthenticationPrincipal Member member,
            @PathVariable Long timeTableId,
            @RequestParam(defaultValue = "false") boolean forceRefresh
    ) {
        return timeTableEvaluationService.streamEvaluation(member.getId(), timeTableId, forceRefresh);
    }
}
