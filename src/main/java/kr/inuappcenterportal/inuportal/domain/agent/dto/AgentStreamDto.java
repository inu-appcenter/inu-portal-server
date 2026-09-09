package kr.inuappcenterportal.inuportal.domain.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "SSE 스트리밍 응답 패킷 DTO")
public record AgentStreamDto(
        @Schema(description = "이벤트 상태 (ROUTING, TOOLS, DELTA, DONE, ERROR)", example = "DELTA")
        String status,

        @Schema(description = "진행 상태 안내 문구", example = "도구를 실행하고 있습니다...")
        String message,

        @Schema(description = "실행된 도구 식별자 목록", example = "[\"CAFETERIA\"]")
        List<String> tools,

        @Schema(description = "도구 실행 결과로 생성된 Generative UI 카드 목록")
        List<UiComponentDto> uiComponents,

        @Schema(description = "실시간 타이핑용 텍스트 조각", example = "오늘 ")
        String delta,

        @Schema(description = "상황 맞춤형 능동 추천 액션 칩 목록", example = "[\"2호관 식당 메뉴 보기\", \"학생식당 위치\"]")
        List<String> suggestedActions,

        @Schema(description = "스트림 종료 사유", example = "stop")
        String finishReason
) {
    public static AgentStreamDto status(String status, String message) {
        return new AgentStreamDto(status, message, null, null, null, null, null);
    }

    public static AgentStreamDto tools(List<String> tools, List<UiComponentDto> uiComponents) {
        return new AgentStreamDto("TOOLS", null, tools, uiComponents, null, null, null);
    }

    public static AgentStreamDto delta(String delta) {
        return new AgentStreamDto("DELTA", null, null, null, delta, null, null);
    }

    public static AgentStreamDto done(List<String> suggestedActions) {
        return new AgentStreamDto("DONE", null, null, null, null, suggestedActions, "stop");
    }

    public static AgentStreamDto error(String message) {
        return new AgentStreamDto("ERROR", message, null, null, null, null, "error");
    }
}
