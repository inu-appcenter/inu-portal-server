package kr.inuappcenterportal.inuportal.global.dto.vllm;

public record VllmChatMessageDto(
        String role,
        String content
) {
    public static VllmChatMessageDto system(String content) {
        return new VllmChatMessageDto("system", content);
    }

    public static VllmChatMessageDto user(String content) {
        return new VllmChatMessageDto("user", content);
    }

    public static VllmChatMessageDto assistant(String content) {
        return new VllmChatMessageDto("assistant", content);
    }
}
