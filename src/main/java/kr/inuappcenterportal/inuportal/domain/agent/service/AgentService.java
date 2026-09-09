package kr.inuappcenterportal.inuportal.domain.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.agent.dto.AgentChatRequestDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.AgentChatResponseDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.AgentToolDecisionDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tools.AgentTools;
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
    private final AgentTools agentTools;
    private final ObjectMapper objectMapper;

    private String buildRoutingPrompt() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();
        int currentDay = today.getDayOfMonth();
        String dayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);

        return String.format("""
                당신은 인천대학교 포털 서비스 INTIP의 똑똑한 AI 캠퍼스 비서입니다.
                사용자의 질문을 분석하여 아래 도구 중 가장 적절한 1개를 선택해 반드시 유효한 JSON 형식으로만 응답하세요.
                
                [현재 시점 기준 정보]
                - 오늘 날짜: %d년 %d월 %d일 (%s)
                - 현재 연도: %d년, 현재 월: %d월
                - 사용자가 '오늘', '이번 달', '다음 달', '내일' 등을 언급할 때는 반드시 위 현재 시점을 기준으로 계산하세요.
                  * '이번 달' 학사일정: month는 %d (현재 월)
                  * '다음 달' 학사일정: month는 %d
                  * 특정 월 언급이 없거나 '이번 달'이면 month는 %d를 사용하세요.
                  * '오늘' 학식: day는 %d (1=월~7=일)
                
                [사용 가능한 도구]
                - WEATHER: 날씨, 기온, 미세먼지, 비, 우산 관련 질문 (params: 없음)
                - CAFETERIA: 학식, 식당, 메뉴, 밥, 점심, 저녁, 기숙사식당 관련 질문 (params: {"cafeteria": "학생식당"|"제1기숙사식당"|"2기숙사 식당"|"2호관(교직원)식당"|"27호관식당"|"사범대식당", "day": 요일(1=월~7=일)})
                - BUS: 셔틀버스, 시내버스, 버스 도착 시간, 정류장 관련 질문 (params: {"stopName": "정문"|"공과대"|"자연대"|"송도역" 등})
                - TIMETABLE: 내 시간표, 오늘 수업, 강의실, 다음 강의 관련 질문 (params: 없음)
                - SCHEDULE: 학사일정, 시험기간, 수강신청/정정 기간, 학과 일정 관련 질문 (params: {"year": %d, "month": %d})
                - NOTICE: 장학금, 대회, 행사, 학과공지, 학교 공지사항 검색 질문 (params: {"query": "검색어(2글자 이상)"})
                - DIRECTORY: 학과사무실, 행정실, 부서 위치, 전화번호, 연락처 질문 (params: {"query": "학과/부서명"})
                - GENERAL: 도구 조회가 필요 없는 단순 인사, 잡담, 정체성 질문 (params: 없음)
                
                [응답 규칙]
                마크다운 백틱(```json) 없이 오직 JSON 텍스트 하나만 출력하세요.
                {"tool": "도구명", "params": { ... }, "thought": "판단 이유"}
                """, currentYear, currentMonth, currentDay, dayOfWeek,
                currentYear, currentMonth,
                currentMonth,
                (currentMonth % 12) + 1,
                currentMonth,
                today.getDayOfWeek().getValue(),
                currentYear, currentMonth);
    }

    public AgentChatResponseDto processChat(AgentChatRequestDto requestDto, Member member) {
        String userMessage = requestDto.message().trim();
        log.info("AI Agent incoming message: '{}', member: {}", userMessage, (member != null ? member.getId() : "GUEST"));

        // 1단계: vLLM(Gemma 4)을 통한 의도 파악 및 도구 라우팅 결정
        AgentToolDecisionDto decision = decideTool(userMessage);
        log.info("AI Agent tool decision: {}, thought: {}", decision.tool(), decision.thought());

        // 2단계: 도구 실행 또는 일반 대화 처리
        if ("GENERAL".equalsIgnoreCase(decision.tool())) {
            return handleGeneralConversation(userMessage);
        }

        AgentTools.ToolResult toolResult = executeTool(decision.tool(), decision.params(), member);

        // 3단계: 도구 결과 바탕으로 자연스러운 친절 요약 답변 생성
        String synthesizedMessage = synthesizeAnswer(userMessage, toolResult.summary());

        return AgentChatResponseDto.of(synthesizedMessage, toolResult.uiComponent());
    }

    private AgentToolDecisionDto decideTool(String userMessage) {
        List<VllmChatMessageDto> messages = List.of(
                VllmChatMessageDto.system(buildRoutingPrompt()),
                VllmChatMessageDto.user(userMessage)
        );

        VllmChatRequestDto request = VllmChatRequestDto.builder()
                .messages(messages)
                .temperature(0.1) // 결정론적 도구 분류를 위해 낮은 temperature
                .maxTokens(200)
                .stream(false)
                .build();

        try {
            String rawJson = vllmService.chat(request).trim();
            // 마크다운 코드 블록 제거 방어 코드
            if (rawJson.startsWith("```")) {
                rawJson = rawJson.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "").trim();
            }

            JsonNode node = objectMapper.readTree(rawJson);
            String tool = node.path("tool").asText("GENERAL");
            String thought = node.path("thought").asText("");

            Map<String, Object> params = new LinkedHashMap<>();
            JsonNode paramsNode = node.path("params");
            if (paramsNode.isObject()) {
                paramsNode.fields().forEachRemaining(entry -> {
                    if (entry.getValue().isInt()) {
                        params.put(entry.getKey(), entry.getValue().asInt());
                    } else {
                        params.put(entry.getKey(), entry.getValue().asText());
                    }
                });
            }

            return new AgentToolDecisionDto(tool, params, thought);
        } catch (Exception e) {
            log.error("도구 라우팅 결정 실패, GENERAL로 대체: {}", e.getMessage(), e);
            return new AgentToolDecisionDto("GENERAL", Map.of(), "도구 결정 실패");
        }
    }

    private AgentTools.ToolResult executeTool(String tool, Map<String, Object> params, Member member) {
        return switch (tool.toUpperCase()) {
            case "WEATHER" -> agentTools.executeWeather();
            case "CAFETERIA" -> agentTools.executeCafeteria(params);
            case "BUS" -> agentTools.executeBus(params);
            case "TIMETABLE" -> agentTools.executeTimeTable(member, params);
            case "SCHEDULE" -> agentTools.executeSchedule(member, params);
            case "NOTICE" -> agentTools.executeNotice(params);
            case "DIRECTORY" -> agentTools.executeDirectory(params);
            default -> new AgentTools.ToolResult("요청하신 도구를 찾을 수 없습니다.", null, null);
        };
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
        if (lower.contains("날씨") || lower.contains("비") || lower.contains("우산") || lower.contains("기온") || lower.contains("미세먼지")) {
            return new AgentToolDecisionDto("WEATHER", Map.of(), "규칙 기반 날씨 매핑");
        }
        if (lower.contains("학식") || lower.contains("메뉴") || lower.contains("식당") || lower.contains("밥") || lower.contains("점심") || lower.contains("저녁")) {
            return new AgentToolDecisionDto("CAFETERIA", Map.of(), "규칙 기반 학식 매핑");
        }
        if (lower.contains("버스") || lower.contains("셔틀") || lower.contains("정류장") || lower.contains("몇 분")) {
            return new AgentToolDecisionDto("BUS", Map.of(), "규칙 기반 버스 매핑");
        }
        if (lower.contains("시간표") || lower.contains("수업") || lower.contains("강의실")) {
            return new AgentToolDecisionDto("TIMETABLE", Map.of(), "규칙 기반 시간표 매핑");
        }
        if (lower.contains("일정") || lower.contains("학사") || lower.contains("시험") || lower.contains("종강") || lower.contains("개강")) {
            return new AgentToolDecisionDto("SCHEDULE", Map.of(), "규칙 기반 일정 매핑");
        }
        if (lower.contains("공지") || lower.contains("장학") || lower.contains("모집") || lower.contains("신청")) {
            return new AgentToolDecisionDto("NOTICE", Map.of("query", msg), "규칙 기반 공지 매핑");
        }
        if (lower.contains("전화") || lower.contains("번호") || lower.contains("과사") || lower.contains("사무실") || lower.contains("연락처")) {
            return new AgentToolDecisionDto("DIRECTORY", Map.of("query", msg), "규칙 기반 연락처 매핑");
        }
        return AgentToolDecisionDto.general("기본 대화로 전환");
    }
}
