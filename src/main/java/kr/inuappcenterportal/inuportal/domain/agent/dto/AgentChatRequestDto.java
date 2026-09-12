package kr.inuappcenterportal.inuportal.domain.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "AI 에이전트 질의 요청 DTO")
public record AgentChatRequestDto(
        @Schema(description = "사용자 자연어 질문", example = "오늘 학생식당 점심 메뉴 뭐야?")
        @NotBlank(message = "메시지를 입력해주세요.")
        String message,

        @Schema(description = "최근 대화 히스토리 목록 (Multi-turn 맥락 유지용)")
        List<ChatMessageDto> history,

        @Schema(description = "클라이언트 기기 보안 영역(SSO)에서 제공된 실시간 컨텍스트 데이터 (학적, LMS 과제 요약 등)")
        java.util.Map<String, Object> clientContext
) {}
