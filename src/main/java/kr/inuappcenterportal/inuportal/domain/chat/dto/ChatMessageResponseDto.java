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
    private String senderHash; // 본인 여부 확인용 해시
    private String content;
    private LocalDateTime createDate; // createdAt 대신 createDate 사용

    @Builder
    public ChatMessageResponseDto(Long messageId, Long roomId, String senderNickname, String senderHash, String content, LocalDateTime createDate) {
        this.messageId = messageId;
        this.roomId = roomId;
        this.senderNickname = senderNickname;
        this.senderHash = senderHash;
        this.content = content;
        this.createDate = createDate;
    }

    public static ChatMessageResponseDto of(ChatMessage chatMessage) {
        return ChatMessageResponseDto.builder()
                .messageId(chatMessage.getId())
                .roomId(chatMessage.getChatRoom().getId())
                .senderNickname(chatMessage.getSenderNickname())
                .senderHash(null) // DB 조회 시에는 null (필요 시 채워넣기)
                .content(chatMessage.getContent())
                .createDate(chatMessage.getCreateDate()) // getCreatedAt() 대신 getCreateDate() 사용
                .build();
    }
}
