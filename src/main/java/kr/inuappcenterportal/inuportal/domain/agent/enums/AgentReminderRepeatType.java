package kr.inuappcenterportal.inuportal.domain.agent.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;

@Getter
@RequiredArgsConstructor
public enum AgentReminderRepeatType {
    EVERYDAY("매일"),
    WEEKDAYS("평일(월~금)"),
    WEEKENDS("주말(토~일)"),
    ONCE("1회성");

    private final String description;

    public boolean matches(DayOfWeek dayOfWeek) {
        return switch (this) {
            case EVERYDAY, ONCE -> true;
            case WEEKDAYS -> dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
            case WEEKENDS -> dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        };
    }
}
