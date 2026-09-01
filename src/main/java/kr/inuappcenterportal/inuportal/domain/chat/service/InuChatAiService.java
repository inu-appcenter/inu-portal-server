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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InuChatAiService {

    private final WebClient webClient;

    @Value("${app.inuchat.base-url:https://ai-server.inuappcenter.kr}")
    private String baseUrl;

    @Value("${app.inuchat.chat-path:/inuchat/chat}")
    private String chatPath;

    @Value("${app.inuchat.timeout-seconds:120}")
    private long timeoutSeconds;

    public Mono<String> requestChat(Long memberId, String question, List<Object> history) {
        String deviceId = memberId != null ? "intip-" + memberId : "intip-" + UUID.randomUUID();
        InuChatRequestDto requestDto = InuChatRequestDto.of(question, history);
        String fullUrl = trimTrailingSlash(baseUrl) + (chatPath.startsWith("/") ? chatPath : "/" + chatPath);

        log.info("InuChat AI 요청 시작: memberId={}, deviceId={}, url={}, question={}",
                memberId, deviceId, fullUrl, question);

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
                    log.error("InuChat AI 호출 실패: memberId={}, question={}, error={}", memberId, question, e.getMessage(), e);
                    return Mono.just("챗불이 응답을 가져오는 중 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
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
}
