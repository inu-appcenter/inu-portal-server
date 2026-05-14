package kr.inuappcenterportal.inuportal.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoom;
import kr.inuappcenterportal.inuportal.domain.chat.enums.ChatRoomStatus;
import kr.inuappcenterportal.inuportal.domain.chat.enums.ChatRoomType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 응답에 포함하지 않음
public class ChatRoomResponseDto {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private int maxCapacity;
    private boolean isAnonymous;
    private ChatRoomType type;
    private ChatRoomStatus status;
    private int currentParticipants;
    private LocalDateTime createDate;
    private String myHash; // 본인 확인용 해시
    private boolean isOwner; // 내가 이 방의 방장인지 여부
    private boolean isOfficial; // 운영자 공식 메시지 여부
    private boolean pushEnabled; // 채팅방 알림 켜짐 여부
    private String friendAlias; // 상대방이 친구일 경우 지정된 별명
    private List<ChatMessageResponseDto> messages; // 메시지 목록

    @Builder
    public ChatRoomResponseDto(Long id, String title, String description, String thumbnailUrl, int maxCapacity, boolean isAnonymous, ChatRoomType type, ChatRoomStatus status, int currentParticipants, LocalDateTime createDate, String myHash, boolean isOwner, boolean isOfficial, boolean pushEnabled, String friendAlias, List<ChatMessageResponseDto> messages) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.maxCapacity = maxCapacity;
        this.isAnonymous = isAnonymous;
        this.type = type;
        this.status = status;
        this.currentParticipants = currentParticipants;
        this.createDate = createDate;
        this.myHash = myHash;
        this.isOwner = isOwner;
        this.isOfficial = isOfficial;
        this.pushEnabled = pushEnabled;
        this.friendAlias = friendAlias;
        this.messages = messages;
    }

    public static ChatRoomResponseDto of(ChatRoom chatRoom, int currentParticipants, boolean isOwner, boolean pushEnabled) {
        return ChatRoomResponseDto.builder()
                .id(chatRoom.getId())
                .title(chatRoom.getTitle())
                .description(chatRoom.getDescription())
                .thumbnailUrl(chatRoom.getThumbnailUrl())
                .maxCapacity(chatRoom.getMaxCapacity())
                .isAnonymous(chatRoom.isAnonymous())
                .type(chatRoom.getType())
                .status(chatRoom.getStatus())
                .currentParticipants(currentParticipants)
                .createDate(chatRoom.getCreateDate())
                .isOwner(isOwner)
                .isOfficial(chatRoom.isOfficial())
                .pushEnabled(pushEnabled)
                .build();
    }

    public static ChatRoomResponseDto of(ChatRoom chatRoom, int currentParticipants, String myHash, boolean isOwner, boolean pushEnabled) {
        return ChatRoomResponseDto.builder()
                .id(chatRoom.getId())
                .title(chatRoom.getTitle())
                .description(chatRoom.getDescription())
                .thumbnailUrl(chatRoom.getThumbnailUrl())
                .maxCapacity(chatRoom.getMaxCapacity())
                .isAnonymous(chatRoom.isAnonymous())
                .type(chatRoom.getType())
                .status(chatRoom.getStatus())
                .currentParticipants(currentParticipants)
                .createDate(chatRoom.getCreateDate())
                .myHash(myHash)
                .isOwner(isOwner)
                .isOfficial(chatRoom.isOfficial())
                .pushEnabled(pushEnabled)
                .build();
    }

    public static ChatRoomResponseDto of(ChatRoom chatRoom, int currentParticipants, String myHash, boolean isOwner, boolean pushEnabled, List<ChatMessageResponseDto> messages) {
        return ChatRoomResponseDto.builder()
                .id(chatRoom.getId())
                .title(chatRoom.getTitle())
                .description(chatRoom.getDescription())
                .thumbnailUrl(chatRoom.getThumbnailUrl())
                .maxCapacity(chatRoom.getMaxCapacity())
                .isAnonymous(chatRoom.isAnonymous())
                .type(chatRoom.getType())
                .status(chatRoom.getStatus())
                .currentParticipants(currentParticipants)
                .createDate(chatRoom.getCreateDate())
                .myHash(myHash)
                .isOwner(isOwner)
                .isOfficial(chatRoom.isOfficial())
                .pushEnabled(pushEnabled)
                .messages(messages)
                .build();
    }
}
