package kr.inuappcenterportal.inuportal.domain.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;

@Schema(description = "AI 에이전트 질의 응답 DTO")
public record AgentChatResponseDto(
        @Schema(description = "자연어 요약 및 응답 메시지")
        String message,
        @Schema(description = "함께 렌더링할 대표 동적 UI 컴포넌트 (없을 경우 null)")
        UiComponentDto uiComponent,
        @Schema(description = "함께 렌더링할 동적 UI 컴포넌트 목록 (다중 도구 실행 시)")
        List<UiComponentDto> uiComponents
) {
    public static AgentChatResponseDto of(String message, UiComponentDto uiComponent) {
        List<UiComponentDto> list = (uiComponent != null) ? List.of(uiComponent) : Collections.emptyList();
        return new AgentChatResponseDto(message, uiComponent, list);
    }

    public static AgentChatResponseDto of(String message, List<UiComponentDto> components) {
        UiComponentDto first = (components != null && !components.isEmpty()) ? components.get(0) : null;
        return new AgentChatResponseDto(message, first, components != null ? components : Collections.emptyList());
    }

    public static AgentChatResponseDto textOnly(String message) {
        return new AgentChatResponseDto(message, null, Collections.emptyList());
    }
}
