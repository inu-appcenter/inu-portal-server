package kr.inuappcenterportal.inuportal.domain.agent.dto;

import kr.inuappcenterportal.inuportal.domain.agent.enums.AgentReminderRepeatType;

public record AgentReminderCreateRequestDto(
        String title,
        String targetTime,
        AgentReminderRepeatType repeatType,
        String targetTool,
        String toolParamsJson,
        String schedulesJson,
        String titleTemplate,
        String bodyTemplate,
        String route
) {
}
