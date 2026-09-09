package kr.inuappcenterportal.inuportal.domain.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.agent.dto.AgentChatRequestDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.AgentChatResponseDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.AgentToolDecisionDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentToolRegistry;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.vllm.VllmChatMessageDto;
import kr.inuappcenterportal.inuportal.global.dto.vllm.VllmChatRequestDto;
import kr.inuappcenterportal.inuportal.global.service.VllmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final VllmService vllmService;
    private final AgentToolRegistry agentToolRegistry;
    private final ObjectMapper objectMapper;

    private String buildRoutingPrompt() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();
        int currentDay = today.getDayOfMonth();
        String dayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);

        return String.format("""
                당신은 인천대학교 포털 서비스 INTIP의 똑똑한 AI 캠퍼스 비서입니다.
                사용자의 질문과 요청을 분석하여 아래 도구 목록에서 적합한 도구들을 찾아 반드시 유효한 JSON 형식으로만 응답하세요.
                사용자의 요청에 여러 가지 작업이 포함되어 있는 경우(예: '오늘 날씨랑 점심 학식 알려줘', '첫 수업 어디고 정문 버스 언제 와?'), 반드시 'tools' 배열에 순서대로 모두 포함하세요.
                
                [현재 시점 기준 정보]
                - 오늘 날짜: %d년 %d월 %d일 (%s)
                - 현재 연도: %d년, 현재 월: %d월
                - 사용자가 '오늘', '이번 달', '다음 달', '내일' 등을 언급할 때는 반드시 위 현재 시점을 기준으로 계산하세요.
                  * '이번 달' 학사일정: month는 %d (현재 월)
                  * '다음 달' 학사일정: month는 %d
                  * 특정 월 언급이 없거나 '이번 달'이면 month는 %d를 사용하세요.
                  * '오늘' 학식: day는 %d (1=월~7=일)
                
                [사용 가능한 도구 카탈로그 (Tool Catalog)]
%s
                - GENERAL: 도구 조회가 필요 없는 단순 인사, 잡담, 정체성 질문 (params: 없음)
                
                [응답 규칙]
                마크다운 백틱(```json) 없이 오직 JSON 텍스트 하나만 출력하세요.
                단일 작업인 경우에도 'tools' 배열에 담아서 출력하세요:
                {"tools": [{"tool": "도구명", "params": { ... }}], "thought": "판단 이유"}
                복합 작업인 경우:
                {"tools": [{"tool": "도구명1", "params": { ... }}, {"tool": "도구명2", "params": { ... }}], "thought": "판단 이유"}
                """, currentYear, currentMonth, currentDay, dayOfWeek,
                currentYear, currentMonth,
                currentMonth,
                (currentMonth % 12) + 1,
                currentMonth,
                today.getDayOfWeek().getValue(),
                agentToolRegistry.generateRoutingPromptCatalog());
    }

    public AgentChatResponseDto processChat(AgentChatRequestDto requestDto, Member member) {
        String userMessage = requestDto.message().trim();
        log.info("AI Agent incoming message: '{}', member: {}", userMessage, (member != null ? member.getId() : "GUEST"));

        // 1단계: vLLM(Gemma 4)을 통한 의도 파악 및 다중 도구 라우팅 결정
        AgentToolDecisionDto decision = decideTool(userMessage);
        List<AgentToolDecisionDto.SingleToolCall> effectiveTools = decision.getEffectiveTools();
        log.info("AI Agent tools decision: {}, count: {}, thought: {}", 
                effectiveTools.stream().map(AgentToolDecisionDto.SingleToolCall::tool).toList(),
                effectiveTools.size(),
                decision.thought());

        // 2단계: 도구가 없는 경우 일반 대화 처리
        if (effectiveTools.isEmpty()) {
            return handleGeneralConversation(userMessage);
        }

        // 3단계: 도구 실행 (순차 실행 및 결과 수집)
        List<UiComponentDto> uiComponents = new ArrayList<>();
        StringBuilder combinedSummaries = new StringBuilder();

        for (int i = 0; i < effectiveTools.size(); i++) {
            AgentToolDecisionDto.SingleToolCall toolCall = effectiveTools.get(i);
            AgentTool.ToolResult result = agentToolRegistry.execute(toolCall.tool(), member, toolCall.params());

            if (result.uiComponent() != null) {
                uiComponents.add(result.uiComponent());
            }

            if (result.summary() != null && !result.summary().isBlank()) {
                if (combinedSummaries.length() > 0) {
                    combinedSummaries.append("\n\n");
                }
                if (effectiveTools.size() > 1) {
                    combinedSummaries.append(String.format("[도구 %d (%s) 실행 결과]:\n%s", 
                            i + 1, toolCall.tool(), result.summary()));
                } else {
                    combinedSummaries.append(result.summary());
                }
            }
        }

        // 4단계: 다중 도구 결과를 바탕으로 통합 자연어 답변 합성
        String synthesizedMessage = synthesizeAnswer(userMessage, combinedSummaries.toString());

        return AgentChatResponseDto.of(synthesizedMessage, uiComponents);
    }

    private AgentToolDecisionDto decideTool(String userMessage) {
        List<VllmChatMessageDto> messages = List.of(
                VllmChatMessageDto.system(buildRoutingPrompt()),
                VllmChatMessageDto.user(userMessage)
        );

        VllmChatRequestDto request = VllmChatRequestDto.builder()
                .messages(messages)
                .temperature(0.1) // 결정론적 도구 분류를 위해 낮은 temperature
                .maxTokens(350)
                .stream(false)
                .build();

        try {
            String rawJson = vllmService.chat(request).trim();
            // 마크다운 코드 블록 제거 방어 코드
            if (rawJson.startsWith("```")) {
                rawJson = rawJson.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "").trim();
            }

            JsonNode node = objectMapper.readTree(rawJson);
            String thought = node.path("thought").asText("");

            List<AgentToolDecisionDto.SingleToolCall> toolList = new ArrayList<>();
            JsonNode toolsNode = node.path("tools");
            if (toolsNode.isArray()) {
                for (JsonNode tNode : toolsNode) {
                    String toolName = tNode.path("tool").asText("");
                    if (!toolName.isBlank() && !"GENERAL".equalsIgnoreCase(toolName)) {
                        Map<String, Object> params = parseParamsNode(tNode.path("params"));
                        toolList.add(new AgentToolDecisionDto.SingleToolCall(toolName, params));
                    }
                }
            }

            // 하위 호환: tools 배열 대신 단일 tool/params로 반환된 경우
            if (toolList.isEmpty() && node.has("tool")) {
                String singleTool = node.path("tool").asText("");
                if (!singleTool.isBlank() && !"GENERAL".equalsIgnoreCase(singleTool)) {
                    Map<String, Object> params = parseParamsNode(node.path("params"));
                    toolList.add(new AgentToolDecisionDto.SingleToolCall(singleTool, params));
                }
            }

            return new AgentToolDecisionDto(toolList, null, null, thought);
        } catch (Exception e) {
            log.error("도구 라우팅 결정 실패, Fallback 규칙으로 대체: {}", e.getMessage(), e);
            return fallbackRuleBasedDecision(userMessage);
        }
    }

    private Map<String, Object> parseParamsNode(JsonNode paramsNode) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (paramsNode != null && paramsNode.isObject()) {
            paramsNode.fields().forEachRemaining(entry -> {
                JsonNode val = entry.getValue();
                if (val.isBoolean()) {
                    params.put(entry.getKey(), val.asBoolean());
                } else if (val.isInt()) {
                    params.put(entry.getKey(), val.asInt());
                } else {
                    params.put(entry.getKey(), val.asText());
                }
            });
        }
        return params;
    }

    private String synthesizeAnswer(String userMessage, String toolSummary) {
        if (toolSummary == null || toolSummary.isBlank()) {
            return "조회된 정보가 없습니다.";
        }

        LocalDate today = LocalDate.now();
        String dateHeader = String.format("현재 시점: %d년 %d월 %d일", today.getYear(), today.getMonthValue(), today.getDayOfMonth());

        String prompt = String.format("""
                당신은 인천대학교 포털 INTIP의 다정하고 스마트한 AI 캠퍼스 비서입니다.
                [%s]
                아래 사용자 질문과 시스템 조회 데이터를 참고하여, 학생에게 친절하고 자연스러운 구어체로 1~3문장 요약 답변을 작성하세요.
                반드시 주어진 시스템 데이터의 실제 날짜와 내용을 바탕으로 답변해야 하며, 다른 날짜나 임의의 월을 지어내지 마세요.
                관련 이모지를 적절히 활용하세요.
                
                [사용자 질문]: %s
                [시스템 데이터 요약]: %s
                """, dateHeader, userMessage, toolSummary);

        List<VllmChatMessageDto> messages = List.of(
                VllmChatMessageDto.user(prompt)
        );

        VllmChatRequestDto request = VllmChatRequestDto.builder()
                .messages(messages)
                .temperature(0.7)
                .maxTokens(300)
                .stream(false)
                .build();

        try {
            String answer = vllmService.chat(request).trim();
            if (!answer.isBlank()) {
                return answer;
            }
        } catch (Exception e) {
            log.warn("최종 답변 요약 생성 실패, 도구 요약 원본 반환: {}", e.getMessage());
        }

        return toolSummary;
    }

    private AgentChatResponseDto handleGeneralConversation(String userMessage) {
        String prompt = """
                당신은 인천대학교 포털 서비스 INTIP의 AI 캠퍼스 비서입니다.
                학식, 버스, 날씨, 시간표, 학사일정, 공지사항, 학과 연락처 등 캠퍼스 생활을 돕는 비서로서,
                친절하고 발랄한 어조로 답변해주세요. 필요 시 학식이나 버스, 공지사항을 물어보라고 유도하세요.
                """;

        List<VllmChatMessageDto> messages = List.of(
                VllmChatMessageDto.system(prompt),
                VllmChatMessageDto.user(userMessage)
        );

        VllmChatRequestDto request = VllmChatRequestDto.builder()
                .messages(messages)
                .temperature(0.7)
                .maxTokens(400)
                .stream(false)
                .build();

        try {
            String answer = vllmService.chat(request).trim();
            return AgentChatResponseDto.textOnly(answer);
        } catch (Exception e) {
            log.error("일반 대화 응답 생성 실패: {}", e.getMessage(), e);
            return AgentChatResponseDto.textOnly("안녕하세요! 인천대학교 AI 캠퍼스 비서입니다. 학식, 버스, 시간표, 공지사항 등에 대해 물어보세요!");
        }
    }

    /**
     * LLM의 JSON 응답이 불안정할 때 동작하는 안전 Fallback 규칙
     */
    private AgentToolDecisionDto fallbackRuleBasedDecision(String msg) {
        String lower = msg.toLowerCase();
        List<AgentToolDecisionDto.SingleToolCall> tools = new ArrayList<>();

        if (lower.contains("채팅") && (lower.contains("알림") || lower.contains("푸시"))) {
            boolean enabled = !lower.contains("꺼") && !lower.contains("해제") && !lower.contains("비활성");
            tools.add(new AgentToolDecisionDto.SingleToolCall("ACTION_CHAT_PUSH", Map.of("enabled", enabled)));
        } else if (lower.contains("브리프") || (lower.contains("아침") && lower.contains("브리핑"))) {
            boolean enabled = !lower.contains("꺼") && !lower.contains("해제");
            tools.add(new AgentToolDecisionDto.SingleToolCall("ACTION_DAILY_BRIEF", Map.of("enabled", enabled, "time", "08:30")));
        } else if (lower.contains("키워드") && (lower.contains("알림") || lower.contains("등록") || lower.contains("추가"))) {
            tools.add(new AgentToolDecisionDto.SingleToolCall("ACTION_NOTICE_KEYWORD", Map.of("keyword", msg)));
        } else if (lower.contains("알림 설정") || lower.contains("내 설정") || lower.contains("내 알림")) {
            tools.add(new AgentToolDecisionDto.SingleToolCall("ACTION_MY_SETTINGS", Map.of()));
        }

        if (lower.contains("날씨") || lower.contains("비") || lower.contains("우산") || lower.contains("기온") || lower.contains("미세먼지")) {
            tools.add(new AgentToolDecisionDto.SingleToolCall("WEATHER", Map.of()));
        }
        if (lower.contains("학식") || lower.contains("메뉴") || lower.contains("식당") || lower.contains("밥") || lower.contains("점심") || lower.contains("저녁")) {
            tools.add(new AgentToolDecisionDto.SingleToolCall("CAFETERIA", Map.of("cafeteria", "전체", "mealType", "AUTO")));
        }
        if (lower.contains("버스") || lower.contains("셔틀") || lower.contains("정류장") || lower.contains("몇 분")) {
            tools.add(new AgentToolDecisionDto.SingleToolCall("BUS", Map.of("stopName", "정문")));
        }
        if (lower.contains("시간표") || lower.contains("수업") || lower.contains("강의실")) {
            tools.add(new AgentToolDecisionDto.SingleToolCall("TIMETABLE", Map.of()));
        }
        if (lower.contains("일정") || lower.contains("학사") || lower.contains("시험") || lower.contains("종강") || lower.contains("개강")) {
            LocalDate now = LocalDate.now();
            tools.add(new AgentToolDecisionDto.SingleToolCall("SCHEDULE", Map.of("year", now.getYear(), "month", now.getMonthValue())));
        }
        if (lower.contains("공지") || lower.contains("장학") || lower.contains("모집")) {
            tools.add(new AgentToolDecisionDto.SingleToolCall("NOTICE", Map.of("query", msg)));
        }
        if (lower.contains("전화") || lower.contains("번호") || lower.contains("과사") || lower.contains("사무실") || lower.contains("연락처")) {
            tools.add(new AgentToolDecisionDto.SingleToolCall("DIRECTORY", Map.of("query", msg)));
        }

        if (tools.isEmpty()) {
            return AgentToolDecisionDto.general("기본 대화로 전환");
        }
        return new AgentToolDecisionDto(tools, null, null, "규칙 기반 매핑");
    }
}
