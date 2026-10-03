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

    /** 도구 구현과 AI가 함께 사용하는 단일 기능 명세. */
    AgentToolDefinition getDefinition();

    default String getName() {
        return getDefinition().name();
    }

    /**
     * 도구 실행 루틴
     */
    ToolResult execute(Member member, Map<String, Object> params);

    /**
     * LLM 라우팅 장애(vLLM 타임아웃/오류) 시 작동하는 규칙 기반 Fallback 매칭 조건
     * 각 도구가 자신이 대응할 수 있는 키워드나 질문 패턴인지 스스로 판단합니다.
     */
    default boolean supportsFallback(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        return false;
    }

    /**
     * Fallback 매칭 시 도구에 전달할 기본 파라미터 생성
     */
    default Map<String, Object> createFallbackParams(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        return java.util.Collections.emptyMap();
    }

    /**
     * 맞춤 루틴/알림(Push Notification) 발송 시 사용할 도구 전용 한 줄 요약 포맷터
     * Zero-LLM으로 0ms 내에 실시간 데이터를 정형화된 푸시 텍스트로 변환합니다.
     */
    default String formatNotification(ToolResult result, Map<String, Object> params) {
        if (result == null || result.summary() == null) {
            return "";
        }
        return result.summary();
    }

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
