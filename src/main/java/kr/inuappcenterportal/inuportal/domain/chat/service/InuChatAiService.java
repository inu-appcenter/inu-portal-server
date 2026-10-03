package kr.inuappcenterportal.inuportal.domain.chat.service;

import kr.inuappcenterportal.inuportal.domain.chat.dto.InuChatRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class InuChatAiService {

    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("\\b(20\\d{2})\\d{4,5}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b01[016789][- ]?\\d{3,4}[- ]?\\d{4}\\b");

    private final WebClient webClient;

    @Value("${app.inuchat.base-url:https://ai-server.inuappcenter.kr}")
    private String baseUrl;

    @Value("${app.inuchat.chat-path:/inuchat/chat}")
    private String chatPath;

    @Value("${app.inuchat.timeout-seconds:120}")
    private long timeoutSeconds;

    public Mono<String> requestChat(Long memberId, String question, List<Object> history) {
        return requestChat(memberId, question, history, null);
    }

    public Mono<String> requestChat(Long memberId, String question, List<Object> history, Map<String, Object> academicContext) {
        String deviceId = memberId != null ? "intip-" + memberId : "intip-" + UUID.randomUUID();
        // INUChat은 외부 AI 서비스이므로 개인 식별자는 전달하지 않는다. 학번은
        // 입학연도(예: 2020학번)만 남기며, 대화 이력은 원문 PII가 섞일 수 있어 넘기지 않는다.
        String safeQuestion = anonymizeForInuChat(question);
        String questionWithContext = safeQuestion + formatAcademicContext(academicContext);
        InuChatRequestDto requestDto = InuChatRequestDto.of(questionWithContext, List.of());
        String fullUrl = trimTrailingSlash(baseUrl) + (chatPath.startsWith("/") ? chatPath : "/" + chatPath);

        log.info("InuChat AI 요청 시작: memberId={}, deviceId={}, url={}, question={}",
                memberId, deviceId, fullUrl, questionWithContext);

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(timeoutSeconds));

        WebClient dedicatedWebClient = webClient.mutate()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        return dedicatedWebClient.post()
                .uri(fullUrl)
                .header("X-Guest-Device-Id", deviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_PLAIN, MediaType.ALL)
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeoutSeconds + 5))
                .doOnSuccess(answer -> log.info("InuChat AI 응답 수신 완료: memberId={}, answerLength={}",
                        memberId, answer != null ? answer.length() : 0))
                .onErrorResume(e -> {
                    log.error("InuChat AI 호출 실패: memberId={}, error={}", memberId, e.getMessage(), e);
                    return Mono.just("챗불이 응답을 가져오는 중 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
                });
    }

    /**
     * inuchat AI 서버의 실시간 스트리밍 응답(HTTP Chunked / StreamingResponse)을
     * 0ms 지연으로 클라이언트 SSE에 다이렉트 토스할 수 있도록 Flux<String> 형태로 반환합니다.
     */
    public reactor.core.publisher.Flux<String> streamChat(Long memberId, String question, List<Object> history, Map<String, Object> academicContext) {
        String deviceId = memberId != null ? "intip-" + memberId : "intip-" + UUID.randomUUID();
        String safeQuestion = anonymizeForInuChat(question);
        String questionWithContext = safeQuestion + formatAcademicContext(academicContext);
        InuChatRequestDto requestDto = InuChatRequestDto.of(questionWithContext, List.of());
        String fullUrl = trimTrailingSlash(baseUrl) + (chatPath.startsWith("/") ? chatPath : "/" + chatPath);

        log.info("InuChat AI 스트리밍 요청 시작: memberId={}, deviceId={}, url={}, question={}",
                memberId, deviceId, fullUrl, questionWithContext);

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(timeoutSeconds));

        WebClient dedicatedWebClient = webClient.mutate()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        return dedicatedWebClient.post()
                .uri(fullUrl)
                .header("X-Guest-Device-Id", deviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_PLAIN, MediaType.ALL)
                .bodyValue(requestDto)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(timeoutSeconds + 5))
                .onErrorResume(e -> {
                    log.error("InuChat AI 스트리밍 호출 실패: memberId={}, error={}", memberId, e.getMessage(), e);
                    return reactor.core.publisher.Flux.just("학사 규정 답변을 가져오는 중 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
                });
    }

    private String trimTrailingSlash(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String anonymizeForInuChat(String question) {
        if (question == null) return "";
        String sanitized = STUDENT_ID_PATTERN.matcher(question).replaceAll("$1학번");
        sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll("[이메일 제외]");
        return PHONE_PATTERN.matcher(sanitized).replaceAll("[전화번호 제외]");
    }

    /** 기존 INUChat question 계약을 유지하며 허용된 비식별 필드만 덧붙인다. */
    private String formatAcademicContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) return "";
        List<String> parts = new java.util.ArrayList<>();
        appendContext(parts, context, "entryYear", "입학연도");
        appendContext(parts, context, "departmentName", "학과");
        appendContext(parts, context, "collegeName", "단과대");
        appendContext(parts, context, "enrollmentStatus", "학적상태");
        appendContext(parts, context, "completedSemesterCount", "이수학기");
        appendContext(parts, context, "acquiredCredits", "취득학점");
        appendContext(parts, context, "gradeAverage", "평점평균");
        return parts.isEmpty() ? "" : "\n\n[비식별 학적 참고정보: " + String.join(", ", parts) + "]";
    }

    private void appendContext(List<String> parts, Map<String, Object> context, String key, String label) {
        Object value = context.get(key);
        if (value != null && !String.valueOf(value).isBlank()) parts.add(label + "=" + value);
    }
}
