package kr.inuappcenterportal.inuportal.domain.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이전 대화 턴 DTO")
public record ChatMessageDto(
        @Schema(description = "발화자 역할 (user 또는 assistant)", example = "user")
        String role,

        @Schema(description = "대화 내용", example = "오늘 점심 학식 뭐야?")
        String content
) {
    public static ChatMessageDto user(String content) {
        return new ChatMessageDto("user", content);
    }

    public static ChatMessageDto assistant(String content) {
        return new ChatMessageDto("assistant", content);
    }
}
