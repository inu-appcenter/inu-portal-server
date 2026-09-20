package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.*;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.dto.req.DailyBriefSettingRequestDto;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.enums.ScheduleScope;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.service.DailyBriefService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyBriefAgentTool implements AgentTool {

    private final DailyBriefService dailyBriefService;

    @Override
    public AgentToolDefinition getDefinition() {
        return new AgentToolDefinition("ACTION_DAILY_BRIEF", "시간표·학사일정 데일리 브리프 수신 설정을 변경합니다.",
                java.util.List.of("데일리 브리프 켜기·끄기", "수신 시간 변경", "학교·학과 일정 범위 변경"),
                java.util.List.of("아침 8시에 데일리 브리프 켜줘", "학과 일정만 브리핑해줘"),
                java.util.List.of("날씨·학식·버스 맞춤 알림은 ACTION_MANAGE_REMINDER", "현재 설정 조회는 ACTION_MY_SETTINGS"),
                Map.of("time", AgentToolParameter.string("수신 시각 HH:mm", false),
                        "enabled", AgentToolParameter.bool("활성화 여부", false),
                        "scope", AgentToolParameter.string("일정 범위", false, "ALL", "SCHOOL_ONLY", "DEPT_ONLY")), true, false);
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        if (member == null) {
            return new ToolResult("데일리 브리프 설정을 변경하려면 로그인이 필요합니다.",
                    UiComponentDto.of("AUTH_REQUIRED", Map.of(), "로그인하기", "/login"), null);
        }

        try {
            String time = (params != null && params.get("time") != null && !String.valueOf(params.get("time")).isBlank())
                    ? String.valueOf(params.get("time")).trim()
                    : "08:30";

            boolean enabled = true;
            if (params != null && params.containsKey("enabled")) {
                Object val = params.get("enabled");
                if (val instanceof Boolean b) {
                    enabled = b;
                } else {
                    enabled = Boolean.parseBoolean(String.valueOf(val));
                }
            }

            ScheduleScope scope = ScheduleScope.ALL;
            if (params != null && params.get("scope") != null) {
                try {
                    scope = ScheduleScope.valueOf(String.valueOf(params.get("scope")).toUpperCase().trim());
                } catch (Exception ignored) {}
            }

            DailyBriefSettingRequestDto req = new DailyBriefSettingRequestDto(
                    enabled, // timetableAlertEnabled
                    enabled, // timetablePreAlertEnabled
                    10,      // timetablePreAlertMinutes
                    enabled, // timetableDailyBriefEnabled
                    time,    // timetableDailyBriefTime
                    enabled, // scheduleAlertEnabled
                    time,    // scheduleDailyBriefTime
                    scope    // scheduleScope
            );
            dailyBriefService.updateSettings(member, req);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("settingType", "DAILY_BRIEF");
            data.put("title", "데일리 브리프 알림");
            data.put("enabled", enabled);
            data.put("time", time);
            data.put("scope", scope.name());
            data.put("statusText", enabled ? (time + " 발송 예약") : "알림 꺼짐");
            data.put("message", enabled
                    ? String.format("매일 아침 %s에 오늘의 시간표와 학사일정 브리핑을 보내드립니다.", time)
                    : "데일리 브리프 알림이 해제되었습니다.");

            UiComponentDto component = UiComponentDto.of("SETTING_RESULT", data, "브리프 알림 설정", "/mypage/notification/daily-brief");
            String summary = enabled
                    ? String.format("데일리 브리프 알림이 매일 아침 %s에 발송되도록 설정되었습니다.", time)
                    : "데일리 브리프 알림이 꺼졌습니다.";

            return new ToolResult(summary, component, data);
        } catch (Exception e) {
            log.error("데일리 브리프 설정 오류: {}", e.getMessage(), e);
            return new ToolResult("데일리 브리프 설정을 변경하는 도중 오류가 발생했습니다.", null, null);
        }
    }

    @Override
    public boolean supportsFallback(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        return lower.contains("브리프") || (lower.contains("아침") && lower.contains("브리핑"));
    }

    @Override
    public Map<String, Object> createFallbackParams(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null) return Map.of("enabled", true, "time", "08:30");
        String lower = message.toLowerCase();
        boolean enabled = !lower.contains("꺼") && !lower.contains("해제");
        return Map.of("enabled", enabled, "time", "08:30");
    }
}
