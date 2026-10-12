package kr.inuappcenterportal.inuportal.domain.agent.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record McpJsonRpcRequest(
        String jsonrpc,
        Object id,
        String method,
        Map<String, Object> params
) {
    public McpJsonRpcRequest {
        if (jsonrpc == null || jsonrpc.isBlank()) {
            jsonrpc = "2.0";
        }
    }
}
