package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.agent.dto.AgentReminderDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.enums.AgentReminderRepeatType;
import kr.inuappcenterportal.inuportal.domain.agent.service.AgentReminderService;
import kr.inuappcenterportal.inuportal.domain.agent.tool.*;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class AgentReminderTool implements AgentTool {

    private final AgentReminderService agentReminderService;
    private final AgentToolRegistry agentToolRegistry;
    private final ObjectMapper objectMapper;

    public AgentReminderTool(
            @Lazy AgentReminderService agentReminderService,
            @Lazy AgentToolRegistry agentToolRegistry,
            ObjectMapper objectMapper
    ) {
        this.agentReminderService = agentReminderService;
        this.agentToolRegistry = agentToolRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentToolDefinition getDefinition() {
        return new AgentToolDefinition("ACTION_MANAGE_REMINDER", "특정 시각에 캠퍼스 정보를 보내는 맞춤 푸시 알림을 관리합니다.",
                List.of("학식·날씨·버스·공지 맞춤 알림 생성", "등록된 맞춤 알림 목록 조회", "알림 ID 또는 대상 도구로 삭제"),
                List.of("매일 11시에 학식 알려줘", "내 맞춤 알림 목록 보여줘", "3번 알림 삭제해줘"),
                List.of("시간표·학사일정 기본 브리프는 ACTION_DAILY_BRIEF", "공지 키워드 발생 알림은 ACTION_NOTICE_KEYWORD"),
                Map.of("action", AgentToolParameter.string("수행 작업", true, "CREATE", "DELETE", "LIST"),
                        "targetTime", AgentToolParameter.string("알림 시각 HH:mm", false),
                        "targetTool", AgentToolParameter.string("알림 데이터 도구", false, "CAFETERIA", "WEATHER", "BUS", "NOTICE"),
                        "toolParams", AgentToolParameter.object("대상 도구에 전달할 중첩 파라미터", false, Map.of()),
                        "title", AgentToolParameter.string("알림 제목", false),
                        "repeatType", AgentToolParameter.string("반복 방식", false, "WEEKDAYS", "EVERYDAY", "ONCE"),
                        "reminderId", AgentToolParameter.integer("삭제할 알림 ID", false)), true, false);
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        if (member == null) {
            return new ToolResult("알림을 예약하거나 관리하려면 로그인이 필요합니다.",
                    UiComponentDto.of("AUTH_REQUIRED", Map.of(), "로그인하기", "/login"), null);
        }

        String action = params != null && params.get("action") != null
                ? String.valueOf(params.get("action")).toUpperCase().trim()
                : "CREATE";

        try {
            if ("LIST".equals(action)) {
                List<AgentReminderDto> list = agentReminderService.getMyReminders(member);
                if (list.isEmpty()) {
                    return new ToolResult("현재 등록된 AI 맞춤 알림이 없습니다. 언제든 원하시는 알림을 말씀해 주세요!", null, Collections.emptyMap());
                }
                StringBuilder sb = new StringBuilder("현재 등록된 맞춤 알림 목록입니다:\n");
                for (int i = 0; i < list.size(); i++) {
                    AgentReminderDto r = list.get(i);
                    sb.append(String.format("%d. [%s] %s (%s, %s)\n",
                            i + 1, r.title(), r.targetTime(), r.repeatTypeDesc(), r.enabled() ? "켜짐" : "꺼짐"));
                }
                Map<String, Object> data = Map.of("reminders", list);
                UiComponentDto ui = UiComponentDto.of("REMINDER_LIST", data, "알림 설정 관리", "/mypage/notification/daily-brief?tab=agent");
                return new ToolResult(sb.toString().trim(), ui, data);
            }

            if ("DELETE".equals(action)) {
                if (params.containsKey("reminderId")) {
                    Long reminderId = Long.valueOf(String.valueOf(params.get("reminderId")));
                    agentReminderService.deleteReminder(reminderId, member);
                    return new ToolResult("요청하신 맞춤 알림이 정상적으로 삭제되었습니다.", null, Map.of("deletedId", reminderId));
                }
                // reminderId가 없으면 제목이나 대상 도구로 조회하여 삭제 시도
                List<AgentReminderDto> list = agentReminderService.getMyReminders(member);
                String targetTool = params.get("targetTool") != null ? String.valueOf(params.get("targetTool")).toUpperCase() : null;
                Optional<AgentReminderDto> match = list.stream()
                        .filter(r -> (targetTool != null && r.targetTool().equalsIgnoreCase(targetTool)))
                        .findFirst();

                if (match.isPresent()) {
                    agentReminderService.deleteReminder(match.get().id(), member);
                    return new ToolResult(String.format("'%s' 맞춤 알림이 삭제되었습니다.", match.get().title()),
                            null, Map.of("deletedId", match.get().id()));
                } else {
                    return new ToolResult("삭제할 알림을 찾지 못했습니다. 데일리 브리프 설정 페이지에서 목록을 확인해 주세요.",
                            UiComponentDto.of("LINK", Map.of("url", "/mypage/notification/daily-brief?tab=agent"), "알림 관리 이동", "/mypage/notification/daily-brief?tab=agent"), null);
                }
            }

            // CREATE
            String targetTool = params != null && params.get("targetTool") != null
                    ? String.valueOf(params.get("targetTool")).toUpperCase().trim()
                    : "";

            // [정직성 가드레일]: 요청한 도구가 실제 시스템에 존재하는지 사전 검증
            if (targetTool.isBlank() || agentToolRegistry.findTool(targetTool).isEmpty()) {
                String toolDesc = targetTool.isBlank() ? "지정되지 않은 기능" : targetTool;
                return new ToolResult(
                        String.format("죄송하지만 인팁에서 '%s' 관련 정보는 아직 맞춤 푸시 알림으로 지원하지 않고 있어요. 현재 학식, 날씨, 버스, 공지사항 알림을 이용하실 수 있습니다.", toolDesc),
                        null,
                        null
                );
            }

            String targetTime = params.get("targetTime") != null ? String.valueOf(params.get("targetTime")).trim() : "08:30";
            String title = params.get("title") != null ? String.valueOf(params.get("title")).trim() : (targetTool + " 맞춤 알림");

            AgentReminderRepeatType repeatType = AgentReminderRepeatType.WEEKDAYS;
            if (params.get("repeatType") != null) {
                try {
                    repeatType = AgentReminderRepeatType.valueOf(String.valueOf(params.get("repeatType")).toUpperCase().trim());
                } catch (Exception ignored) {}
            }

            // 파라미터 JSON 직렬화
            Object rawToolParams = params.get("toolParams");
            String toolParamsJson = rawToolParams != null ? objectMapper.writeValueAsString(rawToolParams) : "{}";

            // 도구별 기본 타이틀 및 본문 템플릿, 이동 경로 자동 추론
            String titleTemplate = "🔔 " + title;
            String bodyTemplate = "";
            String route = "/";

            switch (targetTool) {
                case "CAFETERIA" -> {
                    titleTemplate = "🍱 오늘의 학식 안내";
                    bodyTemplate = "{restaurant}: {mainMenu}";
                    route = "/cafeteria";
                }
                case "WEATHER" -> {
                    titleTemplate = "☀️ 오늘 캠퍼스 날씨 브리핑";
                    bodyTemplate = "현재 송도 기온 {temperature}℃, {weatherStatus}";
                    route = "/weather";
                }
                case "BUS" -> {
                    titleTemplate = "🚌 버스 도착 안내";
                    bodyTemplate = "{busNo}번 버스 도착 정보: {arrivalMessage}";
                    route = "/home/bus";
                }
                case "NOTICE" -> {
                    titleTemplate = "📢 오늘 새 소식 공지";
                    bodyTemplate = "{noticeTitle}";
                    route = "/notice";
                }
            }

            AgentReminderDto created = agentReminderService.createReminder(
                    member,
                    title,
                    targetTime,
                    repeatType,
                    targetTool,
                    toolParamsJson,
                    titleTemplate,
                    bodyTemplate,
                    route
            );

            Map<String, Object> cardData = new LinkedHashMap<>();
            cardData.put("id", created.id());
            cardData.put("title", created.title());
            cardData.put("targetTime", created.targetTime());
            cardData.put("repeatTypeDesc", created.repeatTypeDesc());
            cardData.put("targetTool", created.targetTool());
            cardData.put("route", created.route());
            cardData.put("statusText", created.targetTime() + " 발송 예약");

            UiComponentDto component = UiComponentDto.of(
                    "REMINDER_SETTING_RESULT",
                    cardData,
                    "맞춤 알림 관리",
                    "/mypage/notification/daily-brief?tab=agent"
            );

            String summary = String.format("%s %s에 '%s'이(가) 발송되도록 예약해 드렸어요! 🔔",
                    created.repeatTypeDesc(), created.targetTime(), created.title());

            return new ToolResult(summary, component, cardData);

        } catch (Exception e) {
            log.error("[AgentReminderTool] 알림 관리 처리 실패: {}", e.getMessage(), e);
            return new ToolResult("알림을 설정하는 도중 오류가 발생했습니다: " + e.getMessage(), null, null);
        }
    }

    @Override
    public boolean supportsFallback(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        boolean timedInfo = (lower.contains("알려줘") || lower.contains("알림"))
                && lower.matches(".*(\\d{1,2}시|\\d{1,2}:\\d{2}|아침|점심|저녁).*?")
                && (lower.contains("학식") || lower.contains("날씨") || lower.contains("버스") || lower.contains("공지"));
        boolean manage = lower.contains("맞춤 알림") && (lower.contains("목록") || lower.contains("삭제") || lower.contains("취소"));
        return timedInfo || manage;
    }

    @Override
    public Map<String, Object> createFallbackParams(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        String lower = message == null ? "" : message.toLowerCase();
        if (lower.contains("목록")) return Map.of("action", "LIST");
        if (lower.contains("삭제") || lower.contains("취소")) return Map.of("action", "DELETE");
        String targetTool = lower.contains("날씨") ? "WEATHER" : lower.contains("버스") ? "BUS" : lower.contains("공지") ? "NOTICE" : "CAFETERIA";
        java.util.regex.Matcher time = java.util.regex.Pattern.compile("(\\d{1,2})(?::(\\d{2})|시)").matcher(lower);
        String targetTime = time.find() ? String.format("%02d:%02d", Integer.parseInt(time.group(1)), time.group(2) == null ? 0 : Integer.parseInt(time.group(2))) : "08:30";
        return Map.of("action", "CREATE", "targetTool", targetTool, "targetTime", targetTime, "repeatType", "WEEKDAYS");
    }
}
