package kr.inuappcenterportal.inuportal.global.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.global.config.VllmProperties;
import kr.inuappcenterportal.inuportal.global.dto.vllm.VllmChatMessageDto;
import kr.inuappcenterportal.inuportal.global.dto.vllm.VllmChatRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.function.Consumer;

@Service
@Slf4j
public class VllmService {

    private final VllmProperties vllmProperties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public VllmService(VllmProperties vllmProperties, ObjectMapper objectMapper) {
        this.vllmProperties = vllmProperties;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    /**
     * 용도별 모델명 조회 (예: "timetable", "chat", "vision" 등. 없으면 defaultModel 반환)
     */
    public String getModel(String purpose) {
        return vllmProperties.getModelOrDefault(purpose);
    }

    /**
     * 기본 모델명 조회
     */
    public String getDefaultModel() {
        return vllmProperties.getDefaultModel();
    }

    /**
     * 범용 실시간 SSE 스트리밍 채팅 완성
     *
     * @param requestDto 요청 DTO
     * @param onToken 토큰이 생성될 때마다 호출되는 콜백
     * @param onComplete 스트림이 정상 종료되었을 때 호출되는 콜백
     * @param onError 에러 발생 시 호출되는 콜백
     */
    public void streamChat(
            VllmChatRequestDto requestDto,
            Consumer<String> onToken,
            Runnable onComplete,
            Consumer<Throwable> onError
    ) {
        String targetModel = (requestDto.model() != null && !requestDto.model().isBlank())
                ? requestDto.model()
                : vllmProperties.getDefaultModel();

        VllmChatRequestDto actualRequest = VllmChatRequestDto.builder()
                .model(targetModel)
                .messages(requestDto.messages())
                .temperature(requestDto.temperature() != null ? requestDto.temperature() : 0.7)
                .maxTokens(requestDto.maxTokens() != null ? requestDto.maxTokens() : 800)
                .stream(true)
                .build();

        webClient.post()
                .uri(vllmProperties.getUrl() + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + vllmProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(actualRequest)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .subscribe(
                        chunk -> parseAndProcessChunk(chunk, onToken),
                        err -> {
                            log.error("vLLM streaming error: ", err);
                            onError.accept(err);
                        },
                        onComplete
                );
    }

    /**
     * 범용 동기(단건) 채팅 완성
     */
    public String chat(VllmChatRequestDto requestDto) {
        String targetModel = (requestDto.model() != null && !requestDto.model().isBlank())
                ? requestDto.model()
                : vllmProperties.getDefaultModel();

        VllmChatRequestDto actualRequest = VllmChatRequestDto.builder()
                .model(targetModel)
                .messages(requestDto.messages())
                .temperature(requestDto.temperature() != null ? requestDto.temperature() : 0.7)
                .maxTokens(requestDto.maxTokens() != null ? requestDto.maxTokens() : 800)
                .stream(false)
                .build();

        try {
            String responseBody = webClient.post()
                    .uri(vllmProperties.getUrl() + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + vllmProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(actualRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseBody != null) {
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode choices = root.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    return choices.get(0).path("message").path("content").asText();
                }
            }
        } catch (Exception e) {
            log.error("vLLM sync chat error: ", e);
            throw new RuntimeException("AI 응답 생성 실패", e);
        }
        return "";
    }

    private void parseAndProcessChunk(String rawChunk, Consumer<String> onToken) {
        String[] lines = rawChunk.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("data:")) {
                String jsonData = trimmed.substring(5).trim();
                if ("[DONE]".equals(jsonData)) {
                    return;
                }
                try {
                    JsonNode node = objectMapper.readTree(jsonData);
                    JsonNode choices = node.path("choices");
                    if (choices.isArray() && !choices.isEmpty()) {
                        JsonNode delta = choices.get(0).path("delta");
                        if (delta.has("content")) {
                            String token = delta.get("content").asText();
                            if (token != null && !token.isEmpty()) {
                                onToken.accept(token);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("JSON parse skipped for line: {}", trimmed);
                }
            }
        }
    }
}
