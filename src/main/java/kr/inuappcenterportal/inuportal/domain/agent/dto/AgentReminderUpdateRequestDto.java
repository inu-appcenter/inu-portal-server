package kr.inuappcenterportal.inuportal.domain.agent.dto;

import kr.inuappcenterportal.inuportal.domain.agent.enums.AgentReminderRepeatType;

public record AgentReminderUpdateRequestDto(
        String title,
        String targetTime,
        AgentReminderRepeatType repeatType,
        String toolParamsJson,
        String titleTemplate,
        String bodyTemplate,
        String route,
        Boolean enabled
) {
}
