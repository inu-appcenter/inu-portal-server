package kr.inuappcenterportal.inuportal.domain.chat.dto;

import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatMessage;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ChatMessageResponseDto {
    private Long messageId;
    private Long roomId;
    private String senderNickname;
    private String content;
    private LocalDateTime createDate; // createdAt 대신 createDate 사용

    @Builder
    public ChatMessageResponseDto(Long messageId, Long roomId, String senderNickname, String content, LocalDateTime createDate) {
        this.messageId = messageId;
        this.roomId = roomId;
        this.senderNickname = senderNickname;
        this.content = content;
        this.createDate = createDate;
    }

    public static ChatMessageResponseDto of(ChatMessage chatMessage) {
        return ChatMessageResponseDto.builder()
                .messageId(chatMessage.getId())
                .roomId(chatMessage.getChatRoom().getId())
                .senderNickname(chatMessage.getSenderNickname())
                .content(chatMessage.getContent())
                .createDate(chatMessage.getCreateDate()) // getCreatedAt() 대신 getCreateDate() 사용
                .build();
    }
}
