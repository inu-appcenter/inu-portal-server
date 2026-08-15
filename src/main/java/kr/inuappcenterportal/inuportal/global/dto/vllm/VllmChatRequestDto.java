package kr.inuappcenterportal.inuportal.global.dto.vllm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record VllmChatRequestDto(
        String model,
        List<VllmChatMessageDto> messages,
        Double temperature,
        @JsonProperty("max_tokens")
        Integer maxTokens,
        Boolean stream
) {
    public static VllmChatRequestDto of(String model, List<VllmChatMessageDto> messages, boolean stream) {
        return VllmChatRequestDto.builder()
                .model(model)
                .messages(messages)
                .temperature(0.7)
                .maxTokens(800)
                .stream(stream)
                .build();
    }
}
