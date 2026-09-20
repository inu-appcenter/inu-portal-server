package kr.inuappcenterportal.inuportal.domain.agent.tool;

import java.util.List;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 도구 구현, AI 라우팅, 입력 검증, 문서화가 함께 사용하는 단일 명세. */
public record AgentToolDefinition(
        String name,
        String summary,
        List<String> capabilities,
        List<String> triggerExamples,
        List<String> negativeExamples,
        Map<String, AgentToolParameter> parameters,
        boolean requiresLogin,
        boolean readOnly
) {
    public AgentToolDefinition {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("도구 이름은 필수입니다.");
        if (summary == null || summary.isBlank()) throw new IllegalArgumentException("도구 설명은 필수입니다.");
        name = name.toUpperCase().trim();
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        triggerExamples = triggerExamples == null ? List.of() : List.copyOf(triggerExamples);
        negativeExamples = negativeExamples == null ? List.of() : List.copyOf(negativeExamples);
        parameters = parameters == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
    }
}
