package kr.inuappcenterportal.inuportal.domain.agent.tool;

import java.util.List;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** AI 도구 입력 파라미터의 실행 가능한 JSON Schema 명세. */
public record AgentToolParameter(
        Type type,
        String description,
        boolean required,
        List<String> enumValues,
        Object defaultValue,
        Map<String, AgentToolParameter> properties
) {
    public enum Type { STRING, INTEGER, NUMBER, BOOLEAN, OBJECT, ARRAY }

    public AgentToolParameter {
        enumValues = enumValues == null ? List.of() : List.copyOf(enumValues);
        properties = properties == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }

    public static AgentToolParameter string(String description, boolean required, String... enumValues) {
        return new AgentToolParameter(Type.STRING, description, required, List.of(enumValues), null, Map.of());
    }

    public static AgentToolParameter integer(String description, boolean required) {
        return new AgentToolParameter(Type.INTEGER, description, required, List.of(), null, Map.of());
    }

    public static AgentToolParameter bool(String description, boolean required) {
        return new AgentToolParameter(Type.BOOLEAN, description, required, List.of(), null, Map.of());
    }

    public static AgentToolParameter object(String description, boolean required, Map<String, AgentToolParameter> properties) {
        return new AgentToolParameter(Type.OBJECT, description, required, List.of(), null, properties);
    }
}
