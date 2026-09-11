package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
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
    public String getName() {
        return "ACTION_DAILY_BRIEF";
    }

    @Override
    public String getDescription() {
        return "기본 데일리 브리프(시간표 수업 알림 및 학사일정 브리핑) 수신 시간 및 On/Off 설정 (params: {\"time\": \"HH:mm\", \"enabled\": true|false, \"scope\": \"ALL\"|\"SCHOOL_ONLY\"|\"DEPT_ONLY\"}). (주의: 날씨, 학식, 버스 등 맞춤형 알림 예약은 ACTION_MANAGE_REMINDER를 사용할 것)";
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
}
