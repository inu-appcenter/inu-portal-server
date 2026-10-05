package kr.inuappcenterportal.inuportal.domain.agent.mcp;

import kr.inuappcenterportal.inuportal.domain.agent.mcp.dto.McpJsonRpcRequest;
import kr.inuappcenterportal.inuportal.domain.agent.mcp.dto.McpJsonRpcResponse;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentToolDefinition;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentToolParameter;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class McpControllerTest {

    @Mock
    private AgentToolRegistry agentToolRegistry;

    @Mock
    private AgentTool mockTool;

    @InjectMocks
    private McpController mcpController;

    private AgentToolDefinition sampleDef;

    @BeforeEach
    void setUp() {
        sampleDef = new AgentToolDefinition(
                "TIMETABLE",
                "대표 시간표 조회",
                List.of("수업 조회"),
                List.of("오늘 수업 뭐 있어?"),
                List.of(),
                Map.of("targetDay", AgentToolParameter.string("조회 대상 날짜", false)),
                true,
                true
        );
    }

    @Test
    @DisplayName("GET /mcp 상태 조회 테스트")
    void testGetMcpStatus() {
        given(agentToolRegistry.getAllTools()).willReturn(List.of(mockTool));

        ResponseEntity<Map<String, Object>> response = mcpController.getMcpStatus();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("ok");
        assertThat(response.getBody().get("server")).isEqualTo("inu-portal-server");
        assertThat(response.getBody().get("totalTools")).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /mcp initialize 핸들링 테스트")
    void testInitialize() {
        McpJsonRpcRequest request = new McpJsonRpcRequest("2.0", 1, "initialize", Map.of());

        ResponseEntity<McpJsonRpcResponse> response = mcpController.handleMcp(request, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        McpJsonRpcResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.error()).isNull();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) body.result();
        assertThat(result.get("protocolVersion")).isEqualTo("2024-11-05");
    }

    @Test
    @DisplayName("POST /mcp tools/list 명세 변환 테스트")
    void testToolsList() {
        given(mockTool.getDefinition()).willReturn(sampleDef);
        given(agentToolRegistry.getAllTools()).willReturn(List.of(mockTool));

        McpJsonRpcRequest request = new McpJsonRpcRequest("2.0", 2, "tools/list", Map.of());
        ResponseEntity<McpJsonRpcResponse> response = mcpController.handleMcp(request, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        McpJsonRpcResponse body = response.getBody();
        assertThat(body).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) body.result();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");
        assertThat(tools).hasSize(1);
        assertThat(tools.get(0).get("name")).isEqualTo("TIMETABLE");
        assertThat(tools.get(0)).containsKey("inputSchema");
    }

    @Test
    @DisplayName("POST /mcp tools/list ARRAY 타입 파라미터의 items 스키마 변환 테스트")
    void testToolsListArrayParameter() {
        AgentToolDefinition arrayDef = new AgentToolDefinition(
                "API_COURSE_OFFERINGS",
                "개설 강의 목록 검색",
                List.of("개설 강의"),
                List.of(),
                List.of(),
                Map.of("hyNames", AgentToolParameter.array("수강 학년", false, "1", "2", "3", "4")),
                false,
                true
        );
        given(mockTool.getDefinition()).willReturn(arrayDef);
        given(agentToolRegistry.getAllTools()).willReturn(List.of(mockTool));

        McpJsonRpcRequest request = new McpJsonRpcRequest("2.0", 2, "tools/list", Map.of());
        ResponseEntity<McpJsonRpcResponse> response = mcpController.handleMcp(request, null);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody().result();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");
        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) tools.get(0).get("inputSchema");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> hyNamesProp = (Map<String, Object>) properties.get("hyNames");

        assertThat(hyNamesProp.get("type")).isEqualTo("array");
        assertThat(hyNamesProp).containsKey("items");
        @SuppressWarnings("unchecked")
        Map<String, Object> items = (Map<String, Object>) hyNamesProp.get("items");
        assertThat(items.get("type")).isEqualTo("string");
        @SuppressWarnings("unchecked")
        List<String> enums = (List<String>) items.get("enum");
        assertThat(enums).containsExactly("1", "2", "3", "4");
    }

    @Test
    @DisplayName("POST /mcp tools/call 실행 테스트")
    void testToolsCall() {
        AgentTool.ToolResult toolResult = new AgentTool.ToolResult(
                "오늘 수업은 자료구조(09:00) 1개 있습니다.", null, Map.of("count", 1)
        );
        given(agentToolRegistry.execute(eq("TIMETABLE"), any(), any())).willReturn(toolResult);

        McpJsonRpcRequest request = new McpJsonRpcRequest("2.0", 3, "tools/call", Map.of(
                "name", "TIMETABLE",
                "arguments", Map.of("targetDay", "TODAY")
        ));

        ResponseEntity<McpJsonRpcResponse> response = mcpController.handleMcp(request, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        McpJsonRpcResponse body = response.getBody();
        assertThat(body).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) body.result();
        assertThat(result.get("isError")).isEqualTo(false);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
        assertThat(content).hasSize(1);
        assertThat((String) content.get(0).get("text")).contains("자료구조");
    }
}
