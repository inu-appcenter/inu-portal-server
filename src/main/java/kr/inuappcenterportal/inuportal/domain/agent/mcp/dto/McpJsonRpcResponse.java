package kr.inuappcenterportal.inuportal.domain.agent.mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpJsonRpcResponse(
        String jsonrpc,
        Object id,
        Object result,
        McpError error
) {
    public static McpJsonRpcResponse success(Object id, Object result) {
        return new McpJsonRpcResponse("2.0", id, result, null);
    }

    public static McpJsonRpcResponse error(Object id, int code, String message) {
        return new McpJsonRpcResponse("2.0", id, null, new McpError(code, message, null));
    }

    public static McpJsonRpcResponse error(Object id, int code, String message, Object data) {
        return new McpJsonRpcResponse("2.0", id, null, new McpError(code, message, data));
    }

    public record McpError(int code, String message, Object data) {}
}
