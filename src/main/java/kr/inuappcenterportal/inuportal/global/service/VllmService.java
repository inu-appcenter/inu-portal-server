package kr.inuappcenterportal.inuportal.global.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.global.config.VllmProperties;
import kr.inuappcenterportal.inuportal.global.dto.vllm.VllmChatMessageDto;
import kr.inuappcenterportal.inuportal.global.dto.vllm.VllmChatRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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
     * 기본 설정된 vLLM 모델명 조회
     */
    public String getModel() {
        return vllmProperties.model();
    }

    /**
     * 기본 설정된 vLLM Vision 모델명 조회
     */
    public String getVisionModel() {
        return vllmProperties.getVisionModelOrDefault();
    }

    /**
     * 범용 실시간 SSE 스트리밍 채팅 완성
     *
     * @param requestDto 요청 DTO (model이 null이면 기본 설정 모델 사용)
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
                : (vllmProperties.model() != null ? vllmProperties.model() : "vllm-prod");

        VllmChatRequestDto actualRequest = VllmChatRequestDto.builder()
                .model(targetModel)
                .messages(requestDto.messages())
                .temperature(requestDto.temperature() != null ? requestDto.temperature() : 0.7)
                .maxTokens(requestDto.maxTokens() != null ? requestDto.maxTokens() : 800)
                .stream(true)
                .build();

        String endpointUrl = resolveChatCompletionUrl();
        log.info("vLLM streaming request to: {}, model: {}", endpointUrl, targetModel);

        webClient.post()
                .uri(endpointUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + vllmProperties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(actualRequest)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .subscribe(
                        event -> {
                            String data = event.data();
                            if (data == null || data.isBlank() || "[DONE]".equals(data.trim())) {
                                return;
                            }
                            try {
                                JsonNode node = objectMapper.readTree(data);
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
                                log.debug("vLLM SSE parse error for line: {}", data, e);
                            }
                        },
                        err -> {
                            log.error("vLLM streaming error: ", err);
                            onError.accept(err);
                        },
                        () -> {
                            log.info("vLLM streaming completed for model: {}", targetModel);
                            onComplete.run();
                        }
                );
    }

    /**
     * 범용 동기(단건) 채팅 완성
     */
    public String chat(VllmChatRequestDto requestDto) {
        String targetModel = (requestDto.model() != null && !requestDto.model().isBlank())
                ? requestDto.model()
                : (vllmProperties.model() != null ? vllmProperties.model() : "vllm-prod");

        VllmChatRequestDto actualRequest = VllmChatRequestDto.builder()
                .model(targetModel)
                .messages(requestDto.messages())
                .temperature(requestDto.temperature() != null ? requestDto.temperature() : 0.7)
                .maxTokens(requestDto.maxTokens() != null ? requestDto.maxTokens() : 800)
                .stream(false)
                .build();

        String endpointUrl = resolveChatCompletionUrl();

        try {
            String responseBody = webClient.post()
                    .uri(endpointUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + vllmProperties.apiKey())
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
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("vLLM sync chat error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("AI 응답 생성 실패: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("vLLM sync chat error: ", e);
            throw new RuntimeException("AI 응답 생성 실패", e);
        }
        return "";
    }

    private String resolveChatCompletionUrl() {
        String base = vllmProperties.url();
        if (base == null || base.isBlank()) {
            base = "https://vllm-api.inuappcenter.kr/v1";
        }
        base = base.trim().replaceAll("/+$", "");
        if (!base.endsWith("/v1")) {
            base += "/v1";
        }
        return base + "/chat/completions";
    }
}
