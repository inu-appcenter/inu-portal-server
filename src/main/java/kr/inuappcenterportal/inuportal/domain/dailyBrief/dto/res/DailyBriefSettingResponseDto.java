package kr.inuappcenterportal.inuportal.domain.dailyBrief.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.enums.ScheduleScope;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.model.DailyBriefSetting;

@Schema(description = "Daily Brief 설정 응답 DTO")
public record DailyBriefSettingResponseDto(
        @Schema(description = "시간표 전체 알림 활성화 여부", example = "true")
        boolean timetableAlertEnabled,

        @Schema(description = "수업 시작 전 알림 활성화 여부", example = "true")
        boolean timetablePreAlertEnabled,

        @Schema(description = "수업 시작 몇 분 전 알림", example = "10")
        int timetablePreAlertMinutes,

        @Schema(description = "당일 강의 목록 브리핑 활성화 여부", example = "true")
        boolean timetableDailyBriefEnabled,

        @Schema(description = "당일 강의 목록 브리핑 수신 시간", example = "08:00")
        String timetableDailyBriefTime,

        @Schema(description = "학사일정 전체 알림 활성화 여부", example = "true")
        boolean scheduleAlertEnabled,

        @Schema(description = "학사일정 브리핑 수신 시간", example = "08:30")
        String scheduleDailyBriefTime,

        @Schema(description = "학사일정 알림 수신 대상 범위", example = "ALL")
        ScheduleScope scheduleScope
) {
    public static DailyBriefSettingResponseDto from(DailyBriefSetting setting) {
        return new DailyBriefSettingResponseDto(
                setting.isTimetableAlertEnabled(),
                setting.isTimetablePreAlertEnabled(),
                setting.getTimetablePreAlertMinutes(),
                setting.isTimetableDailyBriefEnabled(),
                setting.getTimetableDailyBriefTime(),
                setting.isScheduleAlertEnabled(),
                setting.getScheduleDailyBriefTime(),
                setting.getScheduleScope()
        );
    }
}
