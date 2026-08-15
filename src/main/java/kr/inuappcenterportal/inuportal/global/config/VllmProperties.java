package kr.inuappcenterportal.inuportal.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "vllm")
public class VllmProperties {

    /**
     * vLLM 서버 엔드포인트 URL
     */
    private String url;

    /**
     * vLLM API 인증 키
     */
    private String apiKey;

    /**
     * vLLM 모델명 (예: vllm-prod)
     */
    private String model;
}
