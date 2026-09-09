package kr.inuappcenterportal.inuportal.domain.agent.tool;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;

import java.util.Map;

/**
 * 인팁 AI 캠퍼스 비서 도구 SPI (Service Provider Interface)
 * 새로운 기능이나 서비스 추가 시 이 인터페이스를 구현하고 @Component를 붙이면
 * AgentService나 프롬프트의 변경 없이 자동으로 에이전트 도구로 등록됩니다.
 */
public interface AgentTool {

    /**
     * 고유 도구 식별자 (예: WEATHER, CAFETERIA, ACTION_CHAT_PUSH)
     */
    String getName();

    /**
     * LLM 라우팅 프롬프트에 제공할 도구 설명 및 파라미터 규격
     */
    String getDescription();

    /**
     * 도구 실행 루틴
     */
    ToolResult execute(Member member, Map<String, Object> params);

    record ToolResult(
            String summary,
            UiComponentDto uiComponent,
            Object rawData
    ) {
        public static ToolResult of(String summary, UiComponentDto uiComponent, Object rawData) {
            return new ToolResult(summary, uiComponent, rawData);
        }

        public static ToolResult textOnly(String summary) {
            return new ToolResult(summary, null, null);
        }
    }
}
