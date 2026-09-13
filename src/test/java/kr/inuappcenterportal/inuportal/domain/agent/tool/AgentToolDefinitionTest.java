package kr.inuappcenterportal.inuportal.domain.agent.tool;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentToolDefinitionTest {

    private final AgentToolPromptRenderer renderer = new AgentToolPromptRenderer();
    private final AgentToolParameterValidator validator = new AgentToolParameterValidator();

    @Test
    void rendererIncludesCapabilitiesBoundariesSchemaAndExecutionTraits() {
        AgentToolDefinition definition = sampleDefinition();

        String catalog = renderer.render(definition);

        assertTrue(catalog.contains("TEST_TOOL"));
        assertTrue(catalog.contains("가능한 작업: 중첩 입력 테스트"));
        assertTrue(catalog.contains("호출 예시: \"테스트해줘\""));
        assertTrue(catalog.contains("호출하지 않는 경우: 조회만 요청할 때"));
        assertTrue(catalog.contains("action:string(필수) enum=[CREATE]"));
        assertTrue(catalog.contains("payload:object(필수)"));
        assertTrue(catalog.contains("로그인: 필요, 실행 성격: 변경 가능"));
    }

    @Test
    void validatorPreservesAndValidatesNestedObjects() {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("action", "CREATE");
        payload.put("payload", Map.of("enabled", true));

        assertTrue(validator.validate(sampleDefinition(), payload).isEmpty());
        assertInstanceOf(Map.class, payload.get("payload"));
    }

    @Test
    void validatorRejectsMissingInvalidAndWrongTypedValues() {
        List<String> missing = validator.validate(sampleDefinition(), Map.of());
        List<String> invalid = validator.validate(sampleDefinition(), Map.of(
                "action", "DELETE",
                "payload", "not-an-object"
        ));

        assertEquals(2, missing.size());
        assertTrue(invalid.stream().anyMatch(error -> error.contains("중 하나")));
        assertTrue(invalid.stream().anyMatch(error -> error.contains("object 형식")));
    }

    @Test
    void routingJsonParserDoesNotFlattenNestedToolParams() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        var json = objectMapper.readTree("""
                {"targetTool":"CAFETERIA","toolParams":{"mealType":"LUNCH"},"days":[1,2,3]}
                """);

        Map<String, Object> parsed = AgentToolJsonParser.toMap(objectMapper, json);

        assertEquals("LUNCH", ((Map<?, ?>) parsed.get("toolParams")).get("mealType"));
        assertEquals(List.of(1, 2, 3), parsed.get("days"));
    }

    private AgentToolDefinition sampleDefinition() {
        return new AgentToolDefinition(
                "TEST_TOOL", "테스트 도구", List.of("중첩 입력 테스트"), List.of("테스트해줘"),
                List.of("조회만 요청할 때"),
                Map.of(
                        "action", AgentToolParameter.string("작업", true, "CREATE"),
                        "payload", AgentToolParameter.object("중첩 값", true,
                                Map.of("enabled", AgentToolParameter.bool("활성화", true)))
                ), true, false
        );
    }
}
