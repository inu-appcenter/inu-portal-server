package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.*;
import kr.inuappcenterportal.inuportal.domain.chat.service.InuChatAiService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * inuai (AppCenter-AI-Server InuChat RAG 엔진) 연동 Agent Tool
 * 삼성 최신 빅스비의 Perplexity 지식 검색 연동 모델을 벤치마킹하여
 * 인천대학교 공식 학칙, 학사 규정, 졸업 요건, 공지사항 상세 RAG 질의응답을 전담합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InuAiAgentTool implements AgentTool {

    private final InuChatAiService inuChatAiService;

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s)\\]]+");
    private static final Pattern ARTICLE_PATTERN = Pattern.compile("(제\\s*\\d+\\s*조(?:의\\s*\\d+)?(?:\\s*\\([^)]+\\))?)");

    @Override
    public AgentToolDefinition getDefinition() {
        return new AgentToolDefinition("INU_AI_KNOWLEDGE", "인천대학교 공식 자료를 근거로 학칙·규정·행정 절차를 답변합니다.",
                List.of("졸업 요건과 조기졸업 규정", "복수전공·부전공·전과 기준", "휴학·복학·학사경고 규정", "장학금 선발 규정", "공식 공지 상세 내용과 행정 절차"),
                List.of("컴퓨터공학부 졸업 요건 알려줘", "휴학은 최대 몇 학기 가능해?", "전과 신청 절차가 뭐야?"),
                List.of("공지 목록 검색은 NOTICE", "개인의 실제 학점·학적 조회는 ACADEMIC", "일정 날짜만 조회하는 경우 SCHEDULE"),
                Map.of("question", AgentToolParameter.string("근거 자료가 필요한 전체 질문", true)), false, true);
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        String question = extractQuestion(params);
        if (question.isBlank()) {
            return new ToolResult("학사 규정 질의 내용이 입력되지 않았습니다.", null, Collections.emptyMap());
        }

        Long memberId = member != null ? member.getId() : null;
        log.info("[InuAiAgentTool] inuai 학사 RAG 엔진 질의 시작: memberId={}, question='{}'", memberId, question);

        try {
            // inuai RAG 엔진 호출 (최대 25초 타임아웃)
            String rawAnswer = inuChatAiService.requestChat(memberId, question, null, selectAcademicContext(question, params))
                    .block(Duration.ofSeconds(25));

            if (rawAnswer == null || rawAnswer.isBlank()) {
                rawAnswer = "해당 학사 규정이나 공지사항에 대해 확인된 지식을 찾지 못했습니다.";
            }

            // 답변에서 출처(URL, 학칙 조항 등) 추출하여 퍼플렉시티 스타일의 Citation 카드 생성
            List<Map<String, String>> citations = extractCitations(rawAnswer);
            Map<String, Object> uiData = new LinkedHashMap<>();
            uiData.put("question", question);
            uiData.put("answer", rawAnswer);
            uiData.put("citations", citations);

            String primaryUrl = !citations.isEmpty() && citations.get(0).containsKey("url")
                    ? citations.get(0).get("url")
                    : "/notice";

            UiComponentDto uiComponent = UiComponentDto.of(
                    "INU_AI_CITATION",
                    uiData,
                    "학칙 및 공지 출처 확인",
                    primaryUrl
            );

            log.info("[InuAiAgentTool] inuai 질의 성공: answerLength={}, citationsCount={}",
                    rawAnswer.length(), citations.size());

            return new ToolResult(rawAnswer, uiComponent, uiData);

        } catch (Exception e) {
            log.error("[InuAiAgentTool] inuai 호출 중 예외 발생: {}", e.getMessage(), e);
            return new ToolResult(
                    "학사 규정 지식 서버 응답이 지연되고 있습니다. 공지사항 게시판 검색을 권장합니다.",
                    null,
                    Map.of("error", e.getMessage() != null ? e.getMessage() : "unknown")
            );
        }
    }

    private String extractQuestion(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        if (params.containsKey("question") && params.get("question") != null) {
            return String.valueOf(params.get("question")).trim();
        }
        if (params.containsKey("query") && params.get("query") != null) {
            return String.valueOf(params.get("query")).trim();
        }
        for (Object v : params.values()) {
            if (v instanceof String s && !s.isBlank()) {
                return s.trim();
            }
        }
        return "";
    }

    /** 질문에 필요한 비식별 학적 정보만 외부 INUChat에 전달한다. */
    private Map<String, Object> selectAcademicContext(String question, Map<String, Object> params) {
        if (params == null || !(params.get("_clientContext") instanceof Map<?, ?> client)
                || !(client.get("academic") instanceof Map<?, ?> academic)) return Map.of();

        String lower = question.toLowerCase();
        boolean graduation = lower.contains("졸업") || lower.contains("수료") || lower.contains("이수") || lower.contains("학점");
        boolean status = lower.contains("휴학") || lower.contains("복학") || lower.contains("전과") || lower.contains("학적");
        boolean scholarship = lower.contains("장학");
        if (!graduation && !status && !scholarship) return Map.of();

        Map<String, Object> selected = new LinkedHashMap<>();
        copyIfPresent(academic, selected, "entryYear", "departmentName", "collegeName", "enrollmentStatus");
        if (graduation || scholarship) {
            copyIfPresent(academic, selected, "completedSemesterCount", "acquiredCredits", "gradeAverage");
        }
        return selected;
    }

    private void copyIfPresent(Map<?, ?> source, Map<String, Object> target, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null && !String.valueOf(value).isBlank()) target.put(key, value);
        }
    }

    private List<Map<String, String>> extractCitations(String answer) {
        List<Map<String, String>> citations = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        // URL 추출
        Matcher urlMatcher = URL_PATTERN.matcher(answer);
        while (urlMatcher.find()) {
            String url = urlMatcher.group();
            if (seenUrls.add(url)) {
                Map<String, String> item = new HashMap<>();
                item.put("type", "URL");
                item.put("title", "관련 학교 공지/원문 바로가기");
                item.put("url", url);
                citations.add(item);
            }
        }

        // 학칙 조항 추출 (예: 제37조, 제14조의2 등)
        Matcher articleMatcher = ARTICLE_PATTERN.matcher(answer);
        Set<String> seenArticles = new HashSet<>();
        while (articleMatcher.find()) {
            String article = articleMatcher.group().trim();
            if (seenArticles.add(article)) {
                Map<String, String> item = new HashMap<>();
                item.put("type", "LAW");
                item.put("title", "인천대학교 학칙 " + article);
                item.put("url", "https://www.inu.ac.kr/inu/1560/subview.do");
                citations.add(item);
            }
        }

        return citations;
    }

    @Override
    public boolean supportsFallback(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();

        // 1. 멀티턴 학사 맥락 확인 (직전 질문이 학칙/규정/졸업이고 학번 입력인 경우)
        if (history != null && !history.isEmpty()) {
            boolean prevWasAcademic = false;
            for (int i = history.size() - 1; i >= 0; i--) {
                kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto h = history.get(i);
                if ("user".equalsIgnoreCase(h.role())) {
                    String pLower = h.content().toLowerCase();
                    if (pLower.contains("졸업") || pLower.contains("학칙") || pLower.contains("규정") || pLower.contains("이수") || pLower.contains("요건")) {
                        prevWasAcademic = true;
                        break;
                    }
                }
            }
            if (prevWasAcademic && lower.matches(".*\\d{2,4}\\s*학번.*")) {
                return true;
            }
        }

        // 2. 일반 학칙, 규정 키워드
        return lower.contains("학칙") || lower.contains("규정") || lower.contains("졸업 요건") || lower.contains("졸업요건") ||
                lower.contains("조기졸업") || lower.contains("조기 졸업") || lower.contains("휴학") || lower.contains("복학") ||
                lower.contains("복수전공") || lower.contains("부전공") || lower.contains("전과") || lower.contains("학사경고") ||
                lower.contains("공학인증") || (lower.contains("졸업") && (lower.contains("가능") || lower.contains("봐줘") || lower.contains("요건") || lower.contains("할 수") || lower.contains("돼")));
    }

    @Override
    public Map<String, Object> createFallbackParams(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null) return Map.of();
        String lower = message.toLowerCase();

        if (history != null && !history.isEmpty() && lower.matches(".*\\d{2,4}\\s*학번.*")) {
            for (int i = history.size() - 1; i >= 0; i--) {
                kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto h = history.get(i);
                if ("user".equalsIgnoreCase(h.role())) {
                    String pLower = h.content().toLowerCase();
                    if (pLower.contains("졸업") || pLower.contains("학칙") || pLower.contains("규정") || pLower.contains("이수") || pLower.contains("요건")) {
                        return Map.of("question", (message + " " + h.content()).trim());
                    }
                }
            }
        }
        return Map.of("question", message.trim());
    }
}
