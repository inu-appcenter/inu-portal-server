package kr.inuappcenterportal.inuportal.domain.agent.dto;

public record AgentReminderTestRequestDto(
        String title,
        String targetTool,
        String toolParamsJson,
        String titleTemplate,
        String bodyTemplate,
        String route
) {
}
