package kr.inuappcenterportal.inuportal.domain.dailyBrief.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.dto.req.DailyBriefSettingRequestDto;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.enums.ScheduleScope;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "daily_brief_setting",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_daily_brief_setting_member",
                        columnNames = {"member_id"}
                )
        }
)
public class DailyBriefSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "timetable_alert_enabled", nullable = false)
    private boolean timetableAlertEnabled = true;

    @Column(name = "timetable_pre_alert_enabled", nullable = false)
    private boolean timetablePreAlertEnabled = true;

    @Column(name = "timetable_pre_alert_minutes", nullable = false)
    private int timetablePreAlertMinutes = 10;

    @Column(name = "timetable_daily_brief_enabled", nullable = false)
    private boolean timetableDailyBriefEnabled = true;

    @Column(name = "timetable_daily_brief_time", nullable = false)
    private String timetableDailyBriefTime = "08:00";

    @Column(name = "schedule_alert_enabled", nullable = false)
    private boolean scheduleAlertEnabled = true;

    @Column(name = "schedule_daily_brief_time", nullable = false)
    private String scheduleDailyBriefTime = "08:30";

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_scope", nullable = false)
    private ScheduleScope scheduleScope = ScheduleScope.ALL;

    @Builder
    public DailyBriefSetting(
            Member member,
            Boolean timetableAlertEnabled,
            Boolean timetablePreAlertEnabled,
            Integer timetablePreAlertMinutes,
            Boolean timetableDailyBriefEnabled,
            String timetableDailyBriefTime,
            Boolean scheduleAlertEnabled,
            String scheduleDailyBriefTime,
            ScheduleScope scheduleScope
    ) {
        this.member = member;
        if (timetableAlertEnabled != null) this.timetableAlertEnabled = timetableAlertEnabled;
        if (timetablePreAlertEnabled != null) this.timetablePreAlertEnabled = timetablePreAlertEnabled;
        if (timetablePreAlertMinutes != null) this.timetablePreAlertMinutes = timetablePreAlertMinutes;
        if (timetableDailyBriefEnabled != null) this.timetableDailyBriefEnabled = timetableDailyBriefEnabled;
        if (timetableDailyBriefTime != null && !timetableDailyBriefTime.isBlank()) this.timetableDailyBriefTime = timetableDailyBriefTime;
        if (scheduleAlertEnabled != null) this.scheduleAlertEnabled = scheduleAlertEnabled;
        if (scheduleDailyBriefTime != null && !scheduleDailyBriefTime.isBlank()) this.scheduleDailyBriefTime = scheduleDailyBriefTime;
        if (scheduleScope != null) this.scheduleScope = scheduleScope;
    }

    public static DailyBriefSetting createDefault(Member member) {
        return DailyBriefSetting.builder()
                .member(member)
                .timetableAlertEnabled(true)
                .timetablePreAlertEnabled(true)
                .timetablePreAlertMinutes(10)
                .timetableDailyBriefEnabled(true)
                .timetableDailyBriefTime("08:00")
                .scheduleAlertEnabled(true)
                .scheduleDailyBriefTime("08:30")
                .scheduleScope(ScheduleScope.ALL)
                .build();
    }

    public void update(DailyBriefSettingRequestDto dto) {
        if (dto.timetableAlertEnabled() != null) {
            this.timetableAlertEnabled = dto.timetableAlertEnabled();
        }
        if (dto.timetablePreAlertEnabled() != null) {
            this.timetablePreAlertEnabled = dto.timetablePreAlertEnabled();
        }
        if (dto.timetablePreAlertMinutes() != null) {
            this.timetablePreAlertMinutes = dto.timetablePreAlertMinutes();
        }
        if (dto.timetableDailyBriefEnabled() != null) {
            this.timetableDailyBriefEnabled = dto.timetableDailyBriefEnabled();
        }
        if (dto.timetableDailyBriefTime() != null && !dto.timetableDailyBriefTime().isBlank()) {
            this.timetableDailyBriefTime = dto.timetableDailyBriefTime();
        }
        if (dto.scheduleAlertEnabled() != null) {
            this.scheduleAlertEnabled = dto.scheduleAlertEnabled();
        }
        if (dto.scheduleDailyBriefTime() != null && !dto.scheduleDailyBriefTime().isBlank()) {
            this.scheduleDailyBriefTime = dto.scheduleDailyBriefTime();
        }
        if (dto.scheduleScope() != null) {
            this.scheduleScope = dto.scheduleScope();
        }
    }
}
