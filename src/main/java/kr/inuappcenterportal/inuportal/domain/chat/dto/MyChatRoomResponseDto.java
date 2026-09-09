package kr.inuappcenterportal.inuportal.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import kr.inuappcenterportal.inuportal.domain.chat.enums.ChatRoomType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

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
    private List<Long> participantProfileImageNumbers; // 참여자들의 프로필 이미지 번호 목록
    private boolean isOwner; // 내가 이 방의 방장인지 여부
    private boolean isOfficial; // 운영자 공식 메시지 여부
    private int currentParticipants; // 현재 참여 인원수
    private String thumbnailUrl; // 채팅방 썸네일 URL
    private String friendAlias; // 상대방이 친구일 경우 지정된 별명
    private boolean pushEnabled; // 채팅방 알림 켜짐 여부

    @Builder
    public MyChatRoomResponseDto(Long roomId, String title, ChatRoomType type, String lastMessage, LocalDateTime lastMessageTime, long unreadCount, String senderName, Long senderProfileImageNumber, List<Long> participantProfileImageNumbers, boolean isOwner, boolean isOfficial, int currentParticipants, String thumbnailUrl, String friendAlias, boolean pushEnabled) {
        this.roomId = roomId;
        this.title = title;
        this.type = type;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.unreadCount = unreadCount;
        this.senderName = senderName;
        this.senderProfileImageNumber = senderProfileImageNumber;
        this.participantProfileImageNumbers = participantProfileImageNumbers;
        this.isOwner = isOwner;
        this.isOfficial = isOfficial;
        this.currentParticipants = currentParticipants;
        this.thumbnailUrl = thumbnailUrl;
        this.friendAlias = friendAlias;
        this.pushEnabled = pushEnabled;
    }
}
