package kr.inuappcenterportal.inuportal.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vllm")
public record VllmProperties(
        String url,
        String apiKey,
        String model
) {
}
