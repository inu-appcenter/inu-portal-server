package kr.inuappcenterportal.inuportal.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

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
     * 기본 LLM 모델명
     */
    private String defaultModel;

    /**
     * 용도별 모델 매핑 (예: chat, timetable, vision 등)
     */
    private Map<String, String> models = new HashMap<>();

    /**
     * 특정 용도의 모델을 조회하고, 없으면 기본 모델을 반환
     */
    public String getModelOrDefault(String purpose) {
        if (models != null && models.containsKey(purpose)) {
            return models.get(purpose);
        }
        return defaultModel;
    }
}
