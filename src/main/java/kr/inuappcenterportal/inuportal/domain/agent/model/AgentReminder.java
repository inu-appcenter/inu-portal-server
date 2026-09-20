package kr.inuappcenterportal.inuportal.domain.agent.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.agent.enums.AgentReminderRepeatType;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "agent_reminder",
        indexes = {
                @Index(name = "idx_agent_reminder_time_enabled", columnList = "target_time, enabled"),
                @Index(name = "idx_agent_reminder_member", columnList = "member_id")
        }
)
public class AgentReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "target_time", nullable = false, length = 10)
    private String targetTime; // "11:00", "08:30" (HH:mm)

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_type", nullable = false, length = 20)
    private AgentReminderRepeatType repeatType = AgentReminderRepeatType.WEEKDAYS;

    @Column(name = "target_tool", nullable = false, length = 50)
    private String targetTool; // "CAFETERIA", "WEATHER", "BUS", etc.

    @Column(name = "tool_params_json", columnDefinition = "TEXT")
    private String toolParamsJson; // "{\"restaurant\":\"DORMITORY_1\",\"mealType\":\"LUNCH\"}"

    @Column(name = "title_template", length = 150)
    private String titleTemplate; // "🍱 11시 학식 메뉴 알림"

    @Column(name = "body_template", columnDefinition = "TEXT")
    private String bodyTemplate; // "{restaurant}: {mainMenu} ({price}원)"

    @Column(name = "route", length = 100)
    private String route; // "/cafeteria", "/home/bus"

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "schedules_json", columnDefinition = "TEXT")
    private String schedulesJson; // "[{\"days\":[\"MON\",\"WED\"],\"time\":\"08:30\"}]"

    @Column(name = "last_sent_date")
    private LocalDate lastSentDate;

    @Column(name = "last_sent_time", length = 10)
    private String lastSentTime; // "08:30"

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public AgentReminder(
            Member member,
            String title,
            String targetTime,
            AgentReminderRepeatType repeatType,
            String targetTool,
            String toolParamsJson,
            String schedulesJson,
            String titleTemplate,
            String bodyTemplate,
            String route,
            Boolean enabled
    ) {
        this.member = member;
        this.title = title;
        this.targetTime = targetTime;
        if (repeatType != null) this.repeatType = repeatType;
        this.targetTool = targetTool;
        this.toolParamsJson = toolParamsJson;
        this.schedulesJson = schedulesJson;
        this.titleTemplate = titleTemplate;
        this.bodyTemplate = bodyTemplate;
        this.route = (route != null && !route.isBlank()) ? route : "/";
        if (enabled != null) this.enabled = enabled;
    }

    public void update(String title, String targetTime, AgentReminderRepeatType repeatType,
                       String toolParamsJson, String schedulesJson, String titleTemplate,
                       String bodyTemplate, String route, Boolean enabled) {
        if (title != null && !title.isBlank()) this.title = title;
        if (targetTime != null && !targetTime.isBlank()) this.targetTime = targetTime;
        if (repeatType != null) this.repeatType = repeatType;
        if (toolParamsJson != null) this.toolParamsJson = toolParamsJson;
        if (schedulesJson != null) this.schedulesJson = schedulesJson;
        if (titleTemplate != null) this.titleTemplate = titleTemplate;
        if (bodyTemplate != null) this.bodyTemplate = bodyTemplate;
        if (route != null) this.route = route;
        if (enabled != null) this.enabled = enabled;
    }

    public void toggleEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void updateTime(String targetTime) {
        if (targetTime != null && !targetTime.isBlank()) {
            this.targetTime = targetTime;
        }
    }

    public void recordSent(LocalDate sentDate) {
        recordSent(sentDate, null);
    }

    public void recordSent(LocalDate sentDate, String sentTime) {
        this.lastSentDate = sentDate;
        this.lastSentTime = sentTime;
        if (this.repeatType == AgentReminderRepeatType.ONCE) {
            this.enabled = false;
        }
    }

    public boolean matchesSchedule(LocalDate date, java.time.LocalTime time, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        if (!this.enabled) {
            return false;
        }
        String timeStr = time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        String dayName3 = dayOfWeek.name().substring(0, 3); // "MON", "TUE", ...

        if (this.schedulesJson != null && !this.schedulesJson.isBlank()) {
            try {
                java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ReminderScheduleDto> schedules =
                        objectMapper.readValue(
                                this.schedulesJson,
                                new com.fasterxml.jackson.core.type.TypeReference<java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ReminderScheduleDto>>() {}
                        );
                if (schedules != null && !schedules.isEmpty()) {
                    for (kr.inuappcenterportal.inuportal.domain.agent.dto.ReminderScheduleDto s : schedules) {
                        if (s.time() != null && s.time().equals(timeStr)) {
                            if (s.days() != null && !s.days().isEmpty()) {
                                if (s.days().stream().anyMatch(d -> d.equalsIgnoreCase(dayName3) || d.equalsIgnoreCase(dayOfWeek.name()))) {
                                    return true;
                                }
                            } else if (s.repeatType() != null) {
                                if (s.repeatType().matches(dayOfWeek)) {
                                    return true;
                                }
                            } else if (this.repeatType != null && this.repeatType.matches(dayOfWeek)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            } catch (Exception ignored) {}
        }

        // Legacy fallback
        return this.targetTime != null
                && this.targetTime.equals(timeStr)
                && this.repeatType != null
                && this.repeatType.matches(dayOfWeek);
    }
}
