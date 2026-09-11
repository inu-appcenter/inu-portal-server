package kr.inuappcenterportal.inuportal.domain.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.agent.dto.*;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentToolRegistry;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.vllm.VllmChatMessageDto;
import kr.inuappcenterportal.inuportal.global.dto.vllm.VllmChatRequestDto;
import kr.inuappcenterportal.inuportal.global.service.VllmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final VllmService vllmService;
    private final AgentToolRegistry agentToolRegistry;
    private final ObjectMapper objectMapper;

    private static final Pattern CHIPS_PATTERN = Pattern.compile("\\[CHIPS:\\s*(.*?)\\]", Pattern.CASE_INSENSITIVE);

    private String buildRoutingPrompt(List<ChatMessageDto> history) {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();
        int currentDay = today.getDayOfMonth();
        String dayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);

        StringBuilder historySection = new StringBuilder();
        if (history != null && !history.isEmpty()) {
            historySection.append("\n[최근 대화 맥락 (Context)]\n");
            int start = Math.max(0, history.size() - 6);
            for (int i = start; i < history.size(); i++) {
                ChatMessageDto h = history.get(i);
                String roleName = "user".equalsIgnoreCase(h.role()) ? "사용자" : "비서";
                historySection.append(String.format("- %s: %s\n", roleName, h.content()));
            }
            historySection.append("(주의: 사용자의 최신 질문에 '거기', '그 식당', '그 공지' 등 생략되거나 지칭된 표현이 있다면 위 최근 대화 맥락을 기반으로 대상 도구와 파라미터를 복원하세요.)\n");
        }

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
                %s
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
                historySection.toString(),
                agentToolRegistry.generateRoutingPromptCatalog());
    }

    /**
     * 동기식(단건) AI 에이전트 질의 처리 (Multi-turn + ReAct 다단계 체이닝 지원)
     */
    public AgentChatResponseDto processChat(AgentChatRequestDto requestDto, Member member) {
        String userMessage = requestDto.message().trim();
        List<ChatMessageDto> history = requestDto.history() != null ? requestDto.history() : Collections.emptyList();
        log.info("AI Agent incoming message: '{}', member: {}, history turns: {}",
                userMessage, (member != null ? member.getId() : "GUEST"), history.size());

        // 1단계: 1차 도구 라우팅 결정 (대화 맥락 반영)
        AgentToolDecisionDto decision = decideTool(userMessage, history);
        List<AgentToolDecisionDto.SingleToolCall> effectiveTools = decision.getEffectiveTools();
        log.info("AI Agent hop 1 tools decision: {}, count: {}, thought: {}", 
                effectiveTools.stream().map(AgentToolDecisionDto.SingleToolCall::tool).toList(),
                effectiveTools.size(),
                decision.thought());

        // 2단계: 도구가 없는 경우 일반 대화 처리
        if (effectiveTools.isEmpty()) {
            return handleGeneralConversation(userMessage, history);
        }

        // 3단계: 1차 도구 실행
        List<UiComponentDto> uiComponents = new ArrayList<>();
        StringBuilder combinedSummaries = new StringBuilder();
        Set<String> executedToolNames = new HashSet<>();

        for (int i = 0; i < effectiveTools.size(); i++) {
            AgentToolDecisionDto.SingleToolCall toolCall = effectiveTools.get(i);
            executedToolNames.add(toolCall.tool().toUpperCase());
            AgentTool.ToolResult result = agentToolRegistry.execute(toolCall.tool(), member, toolCall.params());

            if (result.uiComponent() != null) {
                uiComponents.add(result.uiComponent());
            }
            if (result.summary() != null && !result.summary().isBlank()) {
                if (combinedSummaries.length() > 0) combinedSummaries.append("\n\n");
                combinedSummaries.append(result.summary());
            }
        }

        // 4단계: ReAct 다단계 자율 체이닝 (2차 연계 도구 결정)
        List<AgentToolDecisionDto.SingleToolCall> secondaryTools =
                decideSecondaryTool(userMessage, history, combinedSummaries.toString(), executedToolNames);

        if (!secondaryTools.isEmpty()) {
            log.info("AI Agent ReAct hop 2 chained tools: {}",
                    secondaryTools.stream().map(AgentToolDecisionDto.SingleToolCall::tool).toList());
            for (AgentToolDecisionDto.SingleToolCall toolCall : secondaryTools) {
                executedToolNames.add(toolCall.tool().toUpperCase());
                AgentTool.ToolResult result = agentToolRegistry.execute(toolCall.tool(), member, toolCall.params());

                if (result.uiComponent() != null) {
                    uiComponents.add(result.uiComponent());
                }
                if (result.summary() != null && !result.summary().isBlank()) {
                    if (combinedSummaries.length() > 0) combinedSummaries.append("\n\n");
                    combinedSummaries.append(result.summary());
                }
            }
        }

        // 5단계: 최종 자연어 요약 및 능동 추천 칩 합성
        SynthesizedResult synthesized = synthesizeAnswer(userMessage, history, combinedSummaries.toString());

        return AgentChatResponseDto.of(synthesized.cleanMessage(), uiComponents, synthesized.suggestedActions());
    }

    /**
     * 비동기 SSE 실시간 스트리밍 질의 처리 (선행 UI 카드 전달 + 자연어 토큰 타이핑 + 추천 칩)
     */
    public SseEmitter processChatStream(AgentChatRequestDto requestDto, Member member) {
        String userMessage = requestDto.message().trim();
        List<ChatMessageDto> history = requestDto.history() != null ? requestDto.history() : Collections.emptyList();

        SseEmitter emitter = new SseEmitter(120_000L);
        emitter.onCompletion(() -> log.debug("Agent SSE stream completed for member {}", (member != null ? member.getId() : "GUEST")));
        emitter.onTimeout(() -> {
            log.warn("Agent SSE stream timeout");
            emitter.complete();
        });
        emitter.onError(e -> log.debug("Agent SSE stream error: {}", e.getMessage()));

        CompletableFuture.runAsync(() -> {
            try {
                sendSse(emitter, "status", AgentStreamDto.status("ROUTING", "질문 의도를 분석하고 있습니다..."));

                // 1. 도구 라우팅 결정
                AgentToolDecisionDto decision = decideTool(userMessage, history);
                List<AgentToolDecisionDto.SingleToolCall> effectiveTools = decision.getEffectiveTools();

                if (effectiveTools.isEmpty()) {
                    sendSse(emitter, "status", AgentStreamDto.status("STREAMING", "답변을 작성하고 있습니다..."));
                    streamGeneralConversation(emitter, userMessage, history);
                    return;
                }

                // 2. 1차 도구 실행
                sendSse(emitter, "status", AgentStreamDto.status("EXECUTING", "필요한 캠퍼스 정보를 조회하고 있습니다..."));
                List<UiComponentDto> uiComponents = new ArrayList<>();
                StringBuilder combinedSummaries = new StringBuilder();
                Set<String> executedToolNames = new HashSet<>();

                for (AgentToolDecisionDto.SingleToolCall toolCall : effectiveTools) {
                    executedToolNames.add(toolCall.tool().toUpperCase());
                    AgentTool.ToolResult result = agentToolRegistry.execute(toolCall.tool(), member, toolCall.params());

                    if (result.uiComponent() != null) {
                        uiComponents.add(result.uiComponent());
                    }
                    if (result.summary() != null && !result.summary().isBlank()) {
                        if (combinedSummaries.length() > 0) combinedSummaries.append("\n\n");
                        combinedSummaries.append(result.summary());
                    }
                }

                // 3. ReAct 2차 도구 연계 검사
                List<AgentToolDecisionDto.SingleToolCall> secondaryTools =
                        decideSecondaryTool(userMessage, history, combinedSummaries.toString(), executedToolNames);

                if (!secondaryTools.isEmpty()) {
                    sendSse(emitter, "status", AgentStreamDto.status("CHAINING", "연계 정보를 추가 조회하고 있습니다..."));
                    for (AgentToolDecisionDto.SingleToolCall toolCall : secondaryTools) {
                        executedToolNames.add(toolCall.tool().toUpperCase());
                        AgentTool.ToolResult result = agentToolRegistry.execute(toolCall.tool(), member, toolCall.params());

                        if (result.uiComponent() != null) {
                            uiComponents.add(result.uiComponent());
                        }
                        if (result.summary() != null && !result.summary().isBlank()) {
                            if (combinedSummaries.length() > 0) combinedSummaries.append("\n\n");
                            combinedSummaries.append(result.summary());
                        }
                    }
                }

                // 4. GENERATIVE UI 카드 즉시 선행 전달 (화면에 카드 먼저 렌더링!)
                sendSse(emitter, "tools", AgentStreamDto.tools(new ArrayList<>(executedToolNames), uiComponents));

                // 5. 자연어 요약 스트리밍 및 추천 칩 전달
                sendSse(emitter, "status", AgentStreamDto.status("STREAMING", "답변을 정리하고 있습니다..."));
                streamSynthesisAnswer(emitter, userMessage, history, combinedSummaries.toString());

            } catch (Exception e) {
                log.error("Agent SSE stream processing error: {}", e.getMessage(), e);
                sendSse(emitter, "error", AgentStreamDto.error("처리 중 오류가 발생했습니다: " + e.getMessage()));
                emitter.complete();
            }
        });

        return emitter;
    }

    private AgentToolDecisionDto decideTool(String userMessage, List<ChatMessageDto> history) {
        List<VllmChatMessageDto> messages = List.of(
                VllmChatMessageDto.system(buildRoutingPrompt(history)),
                VllmChatMessageDto.user(userMessage)
        );

        VllmChatRequestDto request = VllmChatRequestDto.builder()
                .messages(messages)
                .temperature(0.1)
                .maxTokens(350)
                .stream(false)
                .build();

        try {
            String rawJson = vllmService.chat(request).trim();
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

    /**
     * ReAct 2차 도구 연계 자율 판단 (1차 조회 결과를 바탕으로 후속 도구 동적 결정)
     */
    private List<AgentToolDecisionDto.SingleToolCall> decideSecondaryTool(
            String userMessage,
            List<ChatMessageDto> history,
            String hop1Summary,
            Set<String> executedToolNames
    ) {
        String lower = userMessage.toLowerCase();
        boolean mayNeedChain = false;
        if (!executedToolNames.contains("BUS") && (lower.contains("버스") || lower.contains("집") || lower.contains("정류장") || lower.contains("하교") || lower.contains("막차"))) {
            mayNeedChain = true;
        }
        if (!executedToolNames.contains("CAFETERIA") && (lower.contains("학식") || lower.contains("밥") || lower.contains("점심") || lower.contains("저녁") || lower.contains("식당") || lower.contains("먹을"))) {
            mayNeedChain = true;
        }
        if (!executedToolNames.contains("TIMETABLE_GAP") && (lower.contains("공강") || lower.contains("쉬는 시간") || lower.contains("여유"))) {
            mayNeedChain = true;
        }

        if (!mayNeedChain || hop1Summary.isBlank()) {
            return Collections.emptyList();
        }

        String prompt = String.format("""
                사용자가 다음과 같은 질문을 했습니다: "%s"
                이미 1차 도구 실행을 통해 다음 정보를 얻었습니다:
                [1차 조회 결과]:
                %s
                
                위 1차 결과의 장소나 시간 정보(예: 마지막 강의 건물이나 종료 시간, 공강 여부)를 참고하여, 사용자의 요청을 완수하기 위해 추가로 실행해야 할 2차 도구와 파라미터를 JSON으로 응답하세요.
                이미 실행된 도구(%s)는 절대 다시 호출하지 마세요.
                추가 도구가 필요 없다면 {"tools": []}로 응답하세요.
                마크다운 백틱 없이 유효한 JSON 형식만 응답하세요:
                {"tools": [{"tool": "도구명", "params": { ... }}]}
                
                도구 목록:
                - BUS (params: {"stopName": "정문"|"공과대"|"자연대"|"송도역" 등})
                - CAFETERIA (params: {"cafeteria": "전체"|"학생식당"|"2호관(교직원)식당"|"27호관식당" 등, "mealType": "LUNCH"|"DINNER"})
                - TIMETABLE_GAP (params: {"day": 1~7})
                - DIRECTORY (params: {"query": "부서명/건물명"})
                """, userMessage, hop1Summary, String.join(", ", executedToolNames));

        List<VllmChatMessageDto> messages = List.of(VllmChatMessageDto.user(prompt));
        VllmChatRequestDto request = VllmChatRequestDto.builder()
                .messages(messages)
                .temperature(0.1)
                .maxTokens(200)
                .stream(false)
                .build();

        try {
            String rawJson = vllmService.chat(request).trim();
            if (rawJson.startsWith("```")) {
                rawJson = rawJson.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "").trim();
            }
            JsonNode node = objectMapper.readTree(rawJson);
            JsonNode toolsNode = node.path("tools");
            List<AgentToolDecisionDto.SingleToolCall> secondaryList = new ArrayList<>();
            if (toolsNode.isArray()) {
                for (JsonNode tNode : toolsNode) {
                    String toolName = tNode.path("tool").asText("").toUpperCase();
                    if (!toolName.isBlank() && !executedToolNames.contains(toolName) && !"GENERAL".equalsIgnoreCase(toolName)) {
                        Map<String, Object> params = parseParamsNode(tNode.path("params"));
                        secondaryList.add(new AgentToolDecisionDto.SingleToolCall(toolName, params));
                    }
                }
            }
            return secondaryList;
        } catch (Exception e) {
            log.debug("ReAct 2차 도구 연계 추론 생략: {}", e.getMessage());
            return Collections.emptyList();
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

    private SynthesizedResult synthesizeAnswer(String userMessage, List<ChatMessageDto> history, String toolSummary) {
        if (toolSummary == null || toolSummary.isBlank()) {
            return new SynthesizedResult("조회된 정보가 없습니다.", getDefaultSuggestedActions());
        }

        LocalDate today = LocalDate.now();
        String dateHeader = String.format("현재 시점: %d년 %d월 %d일", today.getYear(), today.getMonthValue(), today.getDayOfMonth());

        StringBuilder historyContext = new StringBuilder();
        if (history != null && !history.isEmpty()) {
            historyContext.append("\n[최근 대화 흐름]\n");
            int start = Math.max(0, history.size() - 4);
            for (int i = start; i < history.size(); i++) {
                ChatMessageDto h = history.get(i);
                String role = "user".equalsIgnoreCase(h.role()) ? "학생" : "비서";
                historyContext.append(String.format("%s: %s\n", role, h.content()));
            }
        }

        String prompt = String.format("""
                당신은 인천대학교 포털 INTIP의 다정하고 스마트한 AI 캠퍼스 비서입니다.
                [%s]
                %s
                아래 사용자 질문과 시스템 조회 데이터를 참고하여, 학생에게 친절하고 자연스러운 구어체로 1~3문장 요약 답변을 작성하세요.
                반드시 주어진 시스템 데이터의 실제 날짜와 내용을 바탕으로 답변해야 하며, 다른 날짜나 임의의 사실을 지어내지 마세요.
                [정직성 원칙]: 사용자가 요청한 내용 중 시스템에서 지원하지 않거나 불가능하다고 보고된 사항은 절대로 된 것처럼 거짓말하지 말고, 솔직하게 안 되는 이유와 현재 가능한 대안을 친절하게 설명하세요.
                관련 이모지를 적절히 활용하세요.
                
                답변 마지막 줄에 사용자가 이어서 누를 만한 유용한 후속 추천 질문 칩 2~3개를 다음 형식으로 반드시 포함하세요:
                [CHIPS: 칩1, 칩2, 칩3]
                
                [사용자 질문]: %s
                [시스템 데이터 요약]: %s
                """, dateHeader, historyContext.toString(), userMessage, toolSummary);

        List<VllmChatMessageDto> messages = List.of(VllmChatMessageDto.user(prompt));
        VllmChatRequestDto request = VllmChatRequestDto.builder()
                .messages(messages)
                .temperature(0.7)
                .maxTokens(350)
                .stream(false)
                .build();

        try {
            String rawAnswer = vllmService.chat(request).trim();
            if (!rawAnswer.isBlank()) {
                return parseSynthesizedResult(rawAnswer);
            }
        } catch (Exception e) {
            log.warn("최종 답변 요약 생성 실패, 도구 요약 원본 반환: {}", e.getMessage());
        }

        return new SynthesizedResult(toolSummary, getDefaultSuggestedActions());
    }

    private void streamSynthesisAnswer(SseEmitter emitter, String userMessage, List<ChatMessageDto> history, String toolSummary) {
        LocalDate today = LocalDate.now();
        String dateHeader = String.format("현재 시점: %d년 %d월 %d일", today.getYear(), today.getMonthValue(), today.getDayOfMonth());

        StringBuilder historyContext = new StringBuilder();
        if (history != null && !history.isEmpty()) {
            historyContext.append("\n[최근 대화 흐름]\n");
            int start = Math.max(0, history.size() - 4);
            for (int i = start; i < history.size(); i++) {
                ChatMessageDto h = history.get(i);
                String role = "user".equalsIgnoreCase(h.role()) ? "학생" : "비서";
                historyContext.append(String.format("%s: %s\n", role, h.content()));
            }
        }

        String prompt = String.format("""
                당신은 인천대학교 포털 INTIP의 다정하고 스마트한 AI 캠퍼스 비서입니다.
                [%s]
                %s
                아래 사용자 질문과 시스템 조회 데이터를 참고하여, 학생에게 친절하고 자연스러운 구어체로 1~3문장 요약 답변을 작성하세요.
                반드시 주어진 시스템 데이터의 실제 날짜와 내용을 바탕으로 답변해야 하며, 다른 날짜나 임의의 월을 지어내지 마세요.
                관련 이모지를 적절히 활용하세요.
                
                답변 마지막 줄에 사용자가 이어서 누를 만한 유용한 후속 추천 질문 칩 2~3개를 다음 형식으로 반드시 포함하세요:
                [CHIPS: 칩1, 칩2, 칩3]
                
                [사용자 질문]: %s
                [시스템 데이터 요약]: %s
                """, dateHeader, historyContext.toString(), userMessage, toolSummary);

        List<VllmChatMessageDto> messages = List.of(VllmChatMessageDto.user(prompt));
        VllmChatRequestDto request = VllmChatRequestDto.builder()
                .messages(messages)
                .temperature(0.7)
                .maxTokens(350)
                .stream(true)
                .build();

        StringBuilder accumulated = new StringBuilder();
        boolean[] inChipsSection = new boolean[]{false};

        vllmService.streamChat(
                request,
                token -> {
                    accumulated.append(token);
                    if (token.contains("[CHIPS:") || accumulated.toString().contains("[CHIPS:")) {
                        inChipsSection[0] = true;
                    }
                    if (!inChipsSection[0]) {
                        sendSse(emitter, "delta", AgentStreamDto.delta(token));
                    }
                },
                () -> {
                    List<String> chips = extractChips(accumulated.toString());
                    sendSse(emitter, "done", AgentStreamDto.done(chips));
                    emitter.complete();
                },
                err -> {
                    log.error("스트리밍 요약 오류: {}", err.getMessage());
                    sendSse(emitter, "done", AgentStreamDto.done(getDefaultSuggestedActions()));
                    emitter.complete();
                }
        );
    }

    private AgentChatResponseDto handleGeneralConversation(String userMessage, List<ChatMessageDto> history) {
        String prompt = """
                당신은 인천대학교 포털 서비스 INTIP의 AI 캠퍼스 비서입니다.
                학식, 버스, 날씨, 시간표, 공강 분석, 학사일정, 공지사항, 학과 연락처 등 캠퍼스 생활을 돕는 비서로서,
                친절하고 발랄한 어조로 답변해주세요.
                답변 마지막 줄에 [CHIPS: 오늘 학식 메뉴 추천, 정문 버스 도착 시간, 오늘 수업 시간표] 형식으로 추천 질문을 달아주세요.
                """;

        List<VllmChatMessageDto> messages = new ArrayList<>();
        messages.add(VllmChatMessageDto.system(prompt));
        if (history != null) {
            int start = Math.max(0, history.size() - 4);
            for (int i = start; i < history.size(); i++) {
                ChatMessageDto h = history.get(i);
                if ("user".equalsIgnoreCase(h.role())) {
                    messages.add(VllmChatMessageDto.user(h.content()));
                } else {
                    messages.add(VllmChatMessageDto.assistant(h.content()));
                }
            }
        }
        messages.add(VllmChatMessageDto.user(userMessage));

        VllmChatRequestDto request = VllmChatRequestDto.builder()
                .messages(messages)
                .temperature(0.7)
                .maxTokens(400)
                .stream(false)
                .build();

        try {
            String answer = vllmService.chat(request).trim();
            SynthesizedResult res = parseSynthesizedResult(answer);
            return AgentChatResponseDto.textOnly(res.cleanMessage(), res.suggestedActions());
        } catch (Exception e) {
            log.error("일반 대화 응답 생성 실패: {}", e.getMessage(), e);
            return AgentChatResponseDto.textOnly("안녕하세요! 인천대학교 AI 캠퍼스 비서입니다. 학식, 버스, 시간표, 공강 분석, 공지사항 등에 대해 편하게 물어보세요!",
                    getDefaultSuggestedActions());
        }
    }

    private void streamGeneralConversation(SseEmitter emitter, String userMessage, List<ChatMessageDto> history) {
        String prompt = """
                당신은 인천대학교 포털 서비스 INTIP의 AI 캠퍼스 비서입니다.
                학식, 버스, 날씨, 시간표, 공강 분석, 학사일정, 공지사항, 학과 연락처 등 캠퍼스 생활을 돕는 비서로서,
                친절하고 발랄한 어조로 답변해주세요.
                답변 마지막 줄에 [CHIPS: 오늘 학식 메뉴 추천, 정문 버스 도착 시간, 오늘 수업 시간표] 형식으로 추천 질문을 달아주세요.
                """;

        List<VllmChatMessageDto> messages = new ArrayList<>();
        messages.add(VllmChatMessageDto.system(prompt));
        if (history != null) {
            int start = Math.max(0, history.size() - 4);
            for (int i = start; i < history.size(); i++) {
                ChatMessageDto h = history.get(i);
                if ("user".equalsIgnoreCase(h.role())) {
                    messages.add(VllmChatMessageDto.user(h.content()));
                } else {
                    messages.add(VllmChatMessageDto.assistant(h.content()));
                }
            }
        }
        messages.add(VllmChatMessageDto.user(userMessage));

        VllmChatRequestDto request = VllmChatRequestDto.builder()
                .messages(messages)
                .temperature(0.7)
                .maxTokens(400)
                .stream(true)
                .build();

        StringBuilder accumulated = new StringBuilder();
        boolean[] inChipsSection = new boolean[]{false};

        vllmService.streamChat(
                request,
                token -> {
                    accumulated.append(token);
                    if (token.contains("[CHIPS:") || accumulated.toString().contains("[CHIPS:")) {
                        inChipsSection[0] = true;
                    }
                    if (!inChipsSection[0]) {
                        sendSse(emitter, "delta", AgentStreamDto.delta(token));
                    }
                },
                () -> {
                    List<String> chips = extractChips(accumulated.toString());
                    sendSse(emitter, "done", AgentStreamDto.done(chips));
                    emitter.complete();
                },
                err -> {
                    sendSse(emitter, "done", AgentStreamDto.done(getDefaultSuggestedActions()));
                    emitter.complete();
                }
        );
    }

    private void sendSse(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE 전송 실패 (클라이언트 연결 종료): {}", e.getMessage());
        }
    }

    private SynthesizedResult parseSynthesizedResult(String rawText) {
        if (rawText == null) {
            return new SynthesizedResult("", getDefaultSuggestedActions());
        }
        Matcher matcher = CHIPS_PATTERN.matcher(rawText);
        if (matcher.find()) {
            String chipsStr = matcher.group(1).trim();
            String cleanText = rawText.substring(0, matcher.start()).trim();
            List<String> chips = Arrays.stream(chipsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
            return new SynthesizedResult(cleanText, chips.isEmpty() ? getDefaultSuggestedActions() : chips);
        }
        return new SynthesizedResult(rawText.trim(), getDefaultSuggestedActions());
    }

    private List<String> extractChips(String fullText) {
        if (fullText == null) return getDefaultSuggestedActions();
        Matcher matcher = CHIPS_PATTERN.matcher(fullText);
        if (matcher.find()) {
            String chipsStr = matcher.group(1).trim();
            List<String> chips = Arrays.stream(chipsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
            if (!chips.isEmpty()) return chips;
        }
        return getDefaultSuggestedActions();
    }

    private List<String> getDefaultSuggestedActions() {
        return List.of("오늘 학식 메뉴 추천", "정문 버스 도착 정보", "오늘 시간표 및 공강 확인");
    }

    private record SynthesizedResult(String cleanMessage, List<String> suggestedActions) {}

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

        if (lower.contains("공강") || lower.contains("쉬는 시간") || lower.contains("우주공강") || lower.contains("여유 시간")) {
            tools.add(new AgentToolDecisionDto.SingleToolCall("TIMETABLE_GAP", Map.of()));
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
        if (lower.contains("학적") || lower.contains("취득 학점") || lower.contains("취득학점") || lower.contains("이수 학점") || lower.contains("이수학점") || lower.contains("내 학점") || lower.contains("gpa") || lower.contains("평점")) {
            tools.add(new AgentToolDecisionDto.SingleToolCall("ACADEMIC", Map.of()));
        }
        if (lower.contains("도서관") || lower.contains("열람실") || lower.contains("노트북실") || lower.contains("스터디룸") || lower.contains("자리") || lower.contains("좌석") || lower.contains("세미나실")) {
            String target = "SEATS";
            if (lower.contains("스터디룸") || lower.contains("세미나실") || lower.contains("공간")) {
                target = "STUDY_ROOMS";
            } else if (lower.contains("연장")) {
                target = "RENEW";
            } else if (lower.contains("반납") || lower.contains("퇴실")) {
                target = "RETURN";
            } else if (lower.contains("내 자리") || lower.contains("내 좌석") || lower.contains("현재 좌석")) {
                target = "MY_SEAT";
            }
            tools.add(new AgentToolDecisionDto.SingleToolCall("LIBRARY", Map.of("target", target)));
        }
        if (lower.contains("lms") || lower.contains("과제") || lower.contains("사이버캠퍼스") || lower.contains("레포트") || lower.contains("숙제") || lower.contains("온라인 강의") || lower.contains("인강") || lower.contains("진도율") || lower.contains("동영상 강의")) {
            String target = "ASSIGNMENTS";
            if (lower.contains("강좌") || lower.contains("과목") || lower.contains("수강")) {
                target = "COURSES";
            } else if (lower.contains("마감") || lower.contains("다가오는") || lower.contains("남은")) {
                target = "UPCOMING";
            }
            tools.add(new AgentToolDecisionDto.SingleToolCall("LMS", Map.of("target", target)));
        }

        if (tools.isEmpty()) {
            return AgentToolDecisionDto.general("기본 대화로 전환");
        }
        return new AgentToolDecisionDto(tools, null, null, "규칙 기반 매핑");
    }
}
