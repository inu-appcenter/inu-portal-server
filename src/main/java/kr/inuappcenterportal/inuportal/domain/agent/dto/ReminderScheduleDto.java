package kr.inuappcenterportal.inuportal.domain.agent.dto;

import kr.inuappcenterportal.inuportal.domain.agent.enums.AgentReminderRepeatType;

import java.util.List;

public record ReminderScheduleDto(
        List<String> days, // e.g. ["MON", "TUE", "WED", "THU", "FRI"]
        String time,       // e.g. "08:30" (HH:mm)
        AgentReminderRepeatType repeatType
) {
}
