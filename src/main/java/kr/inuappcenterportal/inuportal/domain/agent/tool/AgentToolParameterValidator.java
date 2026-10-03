package kr.inuappcenterportal.inuportal.domain.agent.tool;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 명세와 실제 실행 입력의 불일치를 도구 호출 전에 차단한다. */
@Component
public class AgentToolParameterValidator {

    public List<String> validate(AgentToolDefinition definition, Map<String, Object> params) {
        Map<String, Object> safeParams = params == null ? Map.of() : params;
        List<String> errors = new ArrayList<>();

        for (Map.Entry<String, AgentToolParameter> entry : definition.parameters().entrySet()) {
            String key = entry.getKey();
            AgentToolParameter spec = entry.getValue();
            Object value = safeParams.get(key);
            if (value == null || String.valueOf(value).isBlank()) {
                if (spec.required()) errors.add("필수 파라미터 '" + key + "'가 없습니다.");
                continue;
            }
            if (!hasExpectedType(value, spec.type())) {
                errors.add("파라미터 '" + key + "'는 " + spec.type().name().toLowerCase() + " 형식이어야 합니다.");
                continue;
            }
            if (!spec.enumValues().isEmpty()
                    && spec.enumValues().stream().noneMatch(allowed -> allowed.equalsIgnoreCase(String.valueOf(value)))) {
                errors.add("파라미터 '" + key + "'는 " + spec.enumValues() + " 중 하나여야 합니다.");
            }
            if (spec.type() == AgentToolParameter.Type.OBJECT && value instanceof Map<?, ?> nested) {
                validateNested(key, spec.properties(), nested, errors);
            }
        }
        return errors;
    }

    private boolean hasExpectedType(Object value, AgentToolParameter.Type type) {
        return switch (type) {
            case STRING -> value instanceof String;
            case INTEGER -> value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long;
            case NUMBER -> value instanceof Number;
            case BOOLEAN -> value instanceof Boolean;
            case OBJECT -> value instanceof Map<?, ?>;
            case ARRAY -> value instanceof List<?>;
        };
    }

    private void validateNested(String parent, Map<String, AgentToolParameter> specs, Map<?, ?> values, List<String> errors) {
        for (Map.Entry<String, AgentToolParameter> entry : specs.entrySet()) {
            Object value = values.get(entry.getKey());
            if (value == null && entry.getValue().required()) {
                errors.add("필수 파라미터 '" + parent + "." + entry.getKey() + "'가 없습니다.");
            } else if (value != null && !hasExpectedType(value, entry.getValue().type())) {
                errors.add("파라미터 '" + parent + "." + entry.getKey() + "' 형식이 올바르지 않습니다.");
            }
        }
    }
}
