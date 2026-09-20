package kr.inuappcenterportal.inuportal.domain.agent.tool;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/** 구조화된 도구 명세를 라우팅 모델이 읽는 카탈로그 문장으로 변환한다. */
@Component
public class AgentToolPromptRenderer {
    public String render(AgentToolDefinition definition) {
        StringBuilder out = new StringBuilder("- ").append(definition.name()).append(": ")
                .append(definition.summary());
        if (!definition.capabilities().isEmpty()) {
            out.append("\n  가능한 작업: ").append(String.join("; ", definition.capabilities()));
        }
        if (!definition.triggerExamples().isEmpty()) {
            out.append("\n  호출 예시: ").append(definition.triggerExamples().stream()
                    .map(example -> "\"" + example + "\"").collect(Collectors.joining(", ")));
        }
        if (!definition.negativeExamples().isEmpty()) {
            out.append("\n  호출하지 않는 경우: ").append(String.join("; ", definition.negativeExamples()));
        }
        if (definition.parameters().isEmpty()) {
            out.append("\n  params: 없음");
        } else {
            out.append("\n  params: {")
                    .append(definition.parameters().entrySet().stream().map(this::renderParameter)
                            .collect(Collectors.joining(", ")))
                    .append('}');
        }
        out.append("\n  로그인: ").append(definition.requiresLogin() ? "필요" : "불필요")
                .append(", 실행 성격: ").append(definition.readOnly() ? "조회" : "변경 가능");
        return out.toString();
    }

    private String renderParameter(Map.Entry<String, AgentToolParameter> entry) {
        AgentToolParameter parameter = entry.getValue();
        String allowed = parameter.enumValues().isEmpty() ? "" : " enum=" + parameter.enumValues();
        return entry.getKey() + ":" + parameter.type().name().toLowerCase()
                + (parameter.required() ? "(필수)" : "(선택)") + allowed + " - " + parameter.description();
    }
}
