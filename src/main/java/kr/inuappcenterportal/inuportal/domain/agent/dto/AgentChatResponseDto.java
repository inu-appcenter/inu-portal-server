package kr.inuappcenterportal.inuportal.domain.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 에이전트 질의 응답 DTO")
public record AgentChatResponseDto(
        @Schema(description = "자연어 요약 및 응답 메시지")
        String message,
        @Schema(description = "함께 렌더링할 동적 UI 컴포넌트 (없을 경우 null)")
        UiComponentDto uiComponent
) {
    public static AgentChatResponseDto of(String message, UiComponentDto uiComponent) {
        return new AgentChatResponseDto(message, uiComponent);
    }

    public static AgentChatResponseDto textOnly(String message) {
        return new AgentChatResponseDto(message, null);
    }
}
