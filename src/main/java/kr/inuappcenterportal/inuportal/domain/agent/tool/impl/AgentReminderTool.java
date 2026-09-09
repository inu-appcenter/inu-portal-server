package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.agent.dto.AgentReminderDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.enums.AgentReminderRepeatType;
import kr.inuappcenterportal.inuportal.domain.agent.service.AgentReminderService;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentToolRegistry;
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
    public String getName() {
        return "ACTION_MANAGE_REMINDER";
    }

    @Override
    public String getDescription() {
        return "사용자가 원하는 특정 시각에 학식, 날씨, 버스 등 원하는 도구의 정보를 맞춤형 푸시 알림으로 예약/수정/삭제/조회합니다. "
                + "(params: {\"action\": \"CREATE\"|\"DELETE\"|\"LIST\", \"targetTime\": \"HH:mm\", \"targetTool\": \"CAFETERIA\"|\"WEATHER\"|\"BUS\"|\"NOTICE\", "
                + "\"toolParams\": { ... }, \"title\": \"알림 제목\", \"repeatType\": \"WEEKDAYS\"|\"EVERYDAY\"|\"ONCE\"}). "
                + "예: '오전 11시에 학식 알려줘' -> {\"action\":\"CREATE\",\"targetTime\":\"11:00\",\"targetTool\":\"CAFETERIA\",\"toolParams\":{\"mealType\":\"LUNCH\"},\"title\":\"점심 학식 알림\",\"repeatType\":\"WEEKDAYS\"}";
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
                UiComponentDto ui = UiComponentDto.of("REMINDER_LIST", data, "알림 설정 관리", "/mobile/daily-brief?tab=agent");
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
                            UiComponentDto.of("LINK", Map.of("url", "/mobile/daily-brief?tab=agent"), "알림 관리 이동", "/mobile/daily-brief?tab=agent"), null);
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
                    "/mobile/daily-brief?tab=agent"
            );

            String summary = String.format("%s %s에 '%s'이(가) 발송되도록 예약해 드렸어요! 🔔",
                    created.repeatTypeDesc(), created.targetTime(), created.title());

            return new ToolResult(summary, component, cardData);

        } catch (Exception e) {
            log.error("[AgentReminderTool] 알림 관리 처리 실패: {}", e.getMessage(), e);
            return new ToolResult("알림을 설정하는 도중 오류가 발생했습니다: " + e.getMessage(), null, null);
        }
    }
}
