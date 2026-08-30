package kr.inuappcenterportal.inuportal.domain.dailyBrief.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.enums.ScheduleScope;

@Schema(description = "Daily Brief 설정 변경 요청 DTO")
public record DailyBriefSettingRequestDto(
        @Schema(description = "시간표 전체 알림 활성화 여부", example = "true")
        Boolean timetableAlertEnabled,

        @Schema(description = "수업 시작 전 알림 활성화 여부", example = "true")
        Boolean timetablePreAlertEnabled,

        @Schema(description = "수업 시작 몇 분 전 알림 (10, 20, 30, 60)", example = "10")
        Integer timetablePreAlertMinutes,

        @Schema(description = "당일 강의 목록 브리핑 활성화 여부", example = "true")
        Boolean timetableDailyBriefEnabled,

        @Schema(description = "당일 강의 목록 브리핑 수신 시간 (HH:mm)", example = "08:00")
        String timetableDailyBriefTime,

        @Schema(description = "학사일정 전체 알림 활성화 여부", example = "true")
        Boolean scheduleAlertEnabled,

        @Schema(description = "학사일정 브리핑 수신 시간 (HH:mm)", example = "08:30")
        String scheduleDailyBriefTime,

        @Schema(description = "학사일정 알림 수신 대상 범위 (ALL, SCHOOL_ONLY, DEPT_ONLY)", example = "ALL")
        ScheduleScope scheduleScope
) {
}
