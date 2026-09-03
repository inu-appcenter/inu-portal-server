package kr.inuappcenterportal.inuportal.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vllm")
public record VllmProperties(
        String url,
        String apiKey,
        String model,
        String visionModel
) {
    public String getVisionModelOrDefault() {
        return (visionModel != null && !visionModel.isBlank())
                ? visionModel
                : "Qwen/Qwen2.5-VL-7B-Instruct";
    }
}
