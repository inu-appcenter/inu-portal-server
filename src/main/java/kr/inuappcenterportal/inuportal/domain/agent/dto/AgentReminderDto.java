package kr.inuappcenterportal.inuportal.domain.agent.dto;

import kr.inuappcenterportal.inuportal.domain.agent.enums.AgentReminderRepeatType;
import kr.inuappcenterportal.inuportal.domain.agent.model.AgentReminder;

import java.time.LocalDateTime;

public record AgentReminderDto(
        Long id,
        String title,
        String targetTime,
        AgentReminderRepeatType repeatType,
        String repeatTypeDesc,
        String targetTool,
        String toolParamsJson,
        String titleTemplate,
        String bodyTemplate,
        String route,
        boolean enabled,
        LocalDateTime createdAt
) {
    public static AgentReminderDto from(AgentReminder reminder) {
        return new AgentReminderDto(
                reminder.getId(),
                reminder.getTitle(),
                reminder.getTargetTime(),
                reminder.getRepeatType(),
                reminder.getRepeatType().getDescription(),
                reminder.getTargetTool(),
                reminder.getToolParamsJson(),
                reminder.getTitleTemplate(),
                reminder.getBodyTemplate(),
                reminder.getRoute(),
                reminder.isEnabled(),
                reminder.getCreatedAt()
        );
    }
}
