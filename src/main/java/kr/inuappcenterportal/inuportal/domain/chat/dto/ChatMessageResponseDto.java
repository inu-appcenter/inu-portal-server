package kr.inuappcenterportal.inuportal.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatMessage;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ChatMessageResponseDto {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long messageId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long roomId;
    private String senderNickname;
    private String senderHash; // 본인 여부 확인용 해시
    private String content;
    private int imageCount;
    private int unreadCount;
    private String senderAlias;
    private Long senderId;
    private LocalDateTime createDate; // createdAt 대신 createDate 사용

    @Builder
    public ChatMessageResponseDto(Long messageId, Long roomId, String senderNickname, String senderHash, String content, int imageCount, int unreadCount, String senderAlias, Long senderId, LocalDateTime createDate) {
        this.messageId = messageId;
        this.roomId = roomId;
        this.senderNickname = senderNickname;
        this.senderHash = senderHash;
        this.content = content;
        this.imageCount = imageCount;
        this.unreadCount = unreadCount;
        this.senderAlias = senderAlias;
        this.senderId = senderId;
        this.createDate = createDate;
    }

    public static ChatMessageResponseDto of(ChatMessage chatMessage) {
        return ChatMessageResponseDto.builder()
                .messageId(chatMessage.getId())
                .roomId(chatMessage.getChatRoom().getId())
                .senderNickname(chatMessage.getSenderNickname())
                .senderHash(null) // DB 조회 시에는 null (필요 시 채워넣기)
                .senderId(chatMessage.getSender().getId())
                .content(chatMessage.getContent())
                .imageCount(chatMessage.getImageCount())
                .createDate(chatMessage.getCreateDate()) // getCreatedAt() 대신 getCreateDate() 사용
                .build();
    }
}
