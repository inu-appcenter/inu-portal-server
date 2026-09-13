package kr.inuappcenterportal.inuportal.domain.agent.tool;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentToolRegistryTest {

    @Test
    void catalogAndExecutionUseTheSameDefinition() {
        AgentTool tool = new AgentTool() {
            @Override
            public AgentToolDefinition getDefinition() {
                return new AgentToolDefinition("DEMO", "데모 조회", List.of("값 조회"),
                        List.of("데모 보여줘"), List.of(),
                        Map.of("query", AgentToolParameter.string("검색어", true)), false, true);
            }

            @Override
            public ToolResult execute(Member member, Map<String, Object> params) {
                return ToolResult.textOnly("result=" + params.get("query"));
            }
        };
        AgentToolRegistry registry = new AgentToolRegistry(
                List.of(tool), new AgentToolPromptRenderer(), new AgentToolParameterValidator());

        assertTrue(registry.generateRoutingPromptCatalog().contains("호출 예시: \"데모 보여줘\""));
        assertEquals("result=검색", registry.execute("DEMO", null, Map.of("query", "검색")).summary());
        assertTrue(registry.execute("DEMO", null, Map.of()).summary().contains("필수 파라미터"));
    }

    @Test
    void duplicateAndUnknownNamesAreHandledDeterministically() {
        AgentTool first = textTool("SAME", "first");
        AgentTool second = textTool("SAME", "second");
        AgentToolRegistry registry = new AgentToolRegistry(
                List.of(first, second), new AgentToolPromptRenderer(), new AgentToolParameterValidator());

        assertEquals("second", registry.execute("same", null, Map.of()).summary());
        assertTrue(registry.execute("missing", null, Map.of()).summary().contains("찾을 수 없습니다"));
    }

    private AgentTool textTool(String name, String result) {
        return new AgentTool() {
            @Override
            public AgentToolDefinition getDefinition() {
                return new AgentToolDefinition(name, result, List.of(result), List.of(name), List.of(), Map.of(), false, true);
            }

            @Override
            public ToolResult execute(Member member, Map<String, Object> params) {
                return ToolResult.textOnly(result);
            }
        };
    }
}
