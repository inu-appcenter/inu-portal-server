package kr.inuappcenterportal.inuportal.domain.chat.dto;

import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PublicChatMessageResponseDto {
    private Long messageId;
    private String senderNickname;
    private String content;
    private LocalDateTime createDate;

    @Builder
    public PublicChatMessageResponseDto(Long messageId, String senderNickname, String content, LocalDateTime createDate) {
        this.messageId = messageId;
        this.senderNickname = senderNickname;
        this.content = content;
        this.createDate = createDate;
    }

    public static PublicChatMessageResponseDto from(ChatMessage chatMessage) {
        return PublicChatMessageResponseDto.builder()
                .messageId(chatMessage.getId())
                .senderNickname(chatMessage.getSenderNickname())
                .content(chatMessage.getContent())
                .createDate(chatMessage.getCreateDate())
                .build();
    }
}
