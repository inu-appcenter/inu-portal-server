package kr.inuappcenterportal.inuportal.domain.agent.mcp;

import kr.inuappcenterportal.inuportal.domain.agent.mcp.dto.McpJsonRpcRequest;
import kr.inuappcenterportal.inuportal.domain.agent.mcp.dto.McpJsonRpcResponse;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentToolDefinition;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentToolParameter;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentToolRegistry;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class McpController {

    private final AgentToolRegistry agentToolRegistry;

    @GetMapping(value = {"/mcp", "/api/mcp"})
    public ResponseEntity<Map<String, Object>> getMcpStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "server", "inu-portal-server",
                "protocol", "model-context-protocol",
                "version", "2024-11-05",
                "totalTools", agentToolRegistry.getAllTools().size()
        ));
    }

    @PostMapping(value = {"/mcp", "/api/mcp"})
    public ResponseEntity<McpJsonRpcResponse> handleMcp(
            @RequestBody McpJsonRpcRequest request,
            @AuthenticationPrincipal Member member
    ) {
        if (request == null || request.method() == null) {
            return ResponseEntity.badRequest().body(
                    McpJsonRpcResponse.error(null, -32600, "Invalid Request: method is required")
            );
        }

        Object requestId = request.id();
        String method = request.method().trim();
        Map<String, Object> params = request.params() != null ? request.params() : Collections.emptyMap();

        log.info("[MCP] Method: {}, Id: {}, Member: {}", method, requestId, member != null ? member.getId() : "ANONYMOUS");

        switch (method) {
            case "initialize":
                return ResponseEntity.ok(McpJsonRpcResponse.success(requestId, Map.of(
                        "protocolVersion", "2024-11-05",
                        "capabilities", Map.of(
                                "tools", Map.of("listChanged", false)
                        ),
                        "serverInfo", Map.of(
                                "name", "inu-portal-server",
                                "version", "1.0.0"
                        )
                )));

            case "ping":
                return ResponseEntity.ok(McpJsonRpcResponse.success(requestId, Collections.emptyMap()));

            case "tools/list":
                List<Map<String, Object>> tools = new ArrayList<>();
                for (AgentTool tool : agentToolRegistry.getAllTools()) {
                    AgentToolDefinition def = tool.getDefinition();
                    tools.add(buildMcpToolSchema(def));
                }
                return ResponseEntity.ok(McpJsonRpcResponse.success(requestId, Map.of("tools", tools)));

            case "tools/call":
                String toolName = (String) params.get("name");
                if (toolName == null || toolName.isBlank()) {
                    return ResponseEntity.ok(McpJsonRpcResponse.error(
                            requestId, -32602, "Invalid params: 'name' is required for tools/call"
                    ));
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> arguments = params.get("arguments") instanceof Map<?, ?>
                        ? (Map<String, Object>) params.get("arguments")
                        : Collections.emptyMap();

                AgentTool.ToolResult result = agentToolRegistry.execute(toolName, member, arguments);

                List<Map<String, Object>> content = new ArrayList<>();
                if (result.summary() != null) {
                    content.add(Map.of(
                            "type", "text",
                            "text", result.summary()
                    ));
                }

                Map<String, Object> callResult = new LinkedHashMap<>();
                callResult.put("content", content);
                callResult.put("isError", false);
                if (result.uiComponent() != null) {
                    callResult.put("_uiComponent", result.uiComponent());
                }
                if (result.rawData() != null) {
                    callResult.put("_rawData", result.rawData());
                }

                return ResponseEntity.ok(McpJsonRpcResponse.success(requestId, callResult));

            default:
                log.warn("[MCP] Method not found: {}", method);
                return ResponseEntity.ok(McpJsonRpcResponse.error(
                        requestId, -32601, "Method not found: " + method
                ));
        }
    }

    private Map<String, Object> buildMcpToolSchema(AgentToolDefinition def) {
        Map<String, Object> toolMap = new LinkedHashMap<>();
        toolMap.put("name", def.name());
        
        StringBuilder desc = new StringBuilder(def.summary());
        if (!def.capabilities().isEmpty()) {
            desc.append(" [주요 기능: ").append(String.join(", ", def.capabilities())).append("]");
        }
        toolMap.put("description", desc.toString());

        Map<String, Object> inputSchema = new LinkedHashMap<>();
        inputSchema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> requiredList = new ArrayList<>();

        for (Map.Entry<String, AgentToolParameter> entry : def.parameters().entrySet()) {
            String paramName = entry.getKey();
            AgentToolParameter param = entry.getValue();

            Map<String, Object> propMap = new LinkedHashMap<>();
            propMap.put("type", param.type().name().toLowerCase());
            if (param.description() != null && !param.description().isBlank()) {
                propMap.put("description", param.description());
            }
            if (!param.enumValues().isEmpty()) {
                propMap.put("enum", param.enumValues());
            }
            properties.put(paramName, propMap);

            if (param.required()) {
                requiredList.add(paramName);
            }
        }

        inputSchema.put("properties", properties);
        inputSchema.put("required", requiredList);
        toolMap.put("inputSchema", inputSchema);

        return toolMap;
    }
}
