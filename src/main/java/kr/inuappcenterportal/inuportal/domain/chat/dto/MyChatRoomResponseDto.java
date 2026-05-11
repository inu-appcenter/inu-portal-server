package kr.inuappcenterportal.inuportal.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import kr.inuappcenterportal.inuportal.domain.chat.enums.ChatRoomType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MyChatRoomResponseDto {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long roomId;
    private String title;
    private ChatRoomType type;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private long unreadCount;
    private String senderName; // 마지막 채팅 보낸 사람 닉네임 (또는 상대방 이름)
    private Long senderProfileImageNumber; // 마지막 채팅 보낸 사람 (또는 상대방) 이미지 번호

    @Builder
    public MyChatRoomResponseDto(Long roomId, String title, ChatRoomType type, String lastMessage, LocalDateTime lastMessageTime, long unreadCount, String senderName, Long senderProfileImageNumber) {
        this.roomId = roomId;
        this.title = title;
        this.type = type;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.unreadCount = unreadCount;
        this.senderName = senderName;
        this.senderProfileImageNumber = senderProfileImageNumber;
    }
}
