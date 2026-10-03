package kr.inuappcenterportal.inuportal.domain.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentToolDecisionDto(
        List<SingleToolCall> tools,
        String tool,
        Map<String, Object> params,
        String thought
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SingleToolCall(
            String tool,
            Map<String, Object> params
    ) {}

    public List<SingleToolCall> getEffectiveTools() {
        if (tools != null && !tools.isEmpty()) {
            return tools;
        }
        if (tool != null && !tool.isBlank() && !"GENERAL".equalsIgnoreCase(tool)) {
            return List.of(new SingleToolCall(tool, params != null ? params : Map.of()));
        }
        return Collections.emptyList();
    }

    public static AgentToolDecisionDto general(String thought) {
        return new AgentToolDecisionDto(Collections.emptyList(), "GENERAL", Map.of(), thought);
    }
}
