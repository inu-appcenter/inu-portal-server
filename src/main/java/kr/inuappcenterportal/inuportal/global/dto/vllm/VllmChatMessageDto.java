package kr.inuappcenterportal.inuportal.global.dto.vllm;

public record VllmChatMessageDto(
        String role,
        Object content
) {
    public static VllmChatMessageDto system(String content) {
        return new VllmChatMessageDto("system", content);
    }

    public static VllmChatMessageDto user(String content) {
        return new VllmChatMessageDto("user", content);
    }

    public static VllmChatMessageDto userWithImage(String text, String imageUrl) {
        java.util.List<java.util.Map<String, Object>> contentList = new java.util.ArrayList<>();
        if (text != null && !text.isBlank()) {
            contentList.add(java.util.Map.of("type", "text", "text", text));
        }
        contentList.add(java.util.Map.of("type", "image_url", "image_url", java.util.Map.of("url", imageUrl)));
        return new VllmChatMessageDto("user", contentList);
    }

    public static VllmChatMessageDto assistant(String content) {
        return new VllmChatMessageDto("assistant", content);
    }
}
