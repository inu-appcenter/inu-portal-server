package kr.inuappcenterportal.inuportal.domain.agent.tool;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class AgentToolRegistry {

    private final Map<String, AgentTool> toolMap = new LinkedHashMap<>();
    private final AgentToolPromptRenderer promptRenderer;
    private final AgentToolParameterValidator parameterValidator;

    public AgentToolRegistry(List<AgentTool> tools, AgentToolPromptRenderer promptRenderer,
                             AgentToolParameterValidator parameterValidator) {
        this.promptRenderer = promptRenderer;
        this.parameterValidator = parameterValidator;
        for (AgentTool tool : tools) {
            String key = tool.getName().toUpperCase().trim();
            if (toolMap.containsKey(key)) {
                log.warn("[AgentToolRegistry] 중복된 도구 식별자 등록 감지: {}. 기존 도구를 덮어씁니다.", key);
            }
            toolMap.put(key, tool);
            log.info("[AgentToolRegistry] 도구 등록 완료: {}", key);
        }
    }

    public synchronized void registerDynamicTool(AgentTool tool) {
        if (tool == null) return;
        String key = tool.getName().toUpperCase().trim();
        toolMap.put(key, tool);
        log.info("[AgentToolRegistry] 동적 도구(OpenAPI) 등록 완료: {}", key);
    }

    public Optional<AgentTool> findTool(String toolName) {
        if (toolName == null) return Optional.empty();
        return Optional.ofNullable(toolMap.get(toolName.toUpperCase().trim()));
    }

    public AgentTool.ToolResult execute(String toolName, Member member, Map<String, Object> params) {
        return findTool(toolName)
                .map(tool -> {
                    Map<String, Object> safeParams = params != null ? params : Map.of();
                    List<String> errors = parameterValidator.validate(tool.getDefinition(), safeParams);
                    if (!errors.isEmpty()) {
                        return new AgentTool.ToolResult(
                                "도구 입력값이 올바르지 않습니다: " + String.join(" ", errors), null,
                                Map.of("validationErrors", errors));
                    }
                    return tool.execute(member, safeParams);
                })
                .orElseGet(() -> new AgentTool.ToolResult("요청하신 도구(" + toolName + ")를 찾을 수 없습니다.", null, null));
    }

    public List<AgentTool> getAllTools() {
        return List.copyOf(toolMap.values());
    }

    /**
     * 등록된 모든 도구의 메타데이터를 기반으로 LLM 라우팅 프롬프트용 도구 카탈로그를 동적으로 생성합니다.
     */
    public String generateRoutingPromptCatalog() {
        StringBuilder sb = new StringBuilder();
        for (AgentTool tool : toolMap.values()) {
            sb.append(promptRenderer.render(tool.getDefinition())).append('\n');
        }
        return sb.toString().trim();
    }
}
