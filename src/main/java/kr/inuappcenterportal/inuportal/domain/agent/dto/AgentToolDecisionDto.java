package kr.inuappcenterportal.inuportal.domain.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentToolDecisionDto(
        String tool,
        Map<String, Object> params,
        String thought
) {
    public static AgentToolDecisionDto general(String thought) {
        return new AgentToolDecisionDto("GENERAL", Map.of(), thought);
    }
}
