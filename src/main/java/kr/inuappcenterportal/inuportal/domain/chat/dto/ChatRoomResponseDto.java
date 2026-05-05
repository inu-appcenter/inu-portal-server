package kr.inuappcenterportal.inuportal.domain.chat.dto;

import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoom;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ChatRoomResponseDto {
    private Long id;
    private String title;
    private int maxCapacity;
    private boolean isAnonymous;
    private int currentParticipants; // Redis에서 가져올 현재 참여자 수
    private LocalDateTime createDate; // createdAt 대신 createDate 사용

    @Builder
    public ChatRoomResponseDto(Long id, String title, int maxCapacity, boolean isAnonymous, int currentParticipants, LocalDateTime createDate) {
        this.id = id;
        this.title = title;
        this.maxCapacity = maxCapacity;
        this.isAnonymous = isAnonymous;
        this.currentParticipants = currentParticipants;
        this.createDate = createDate;
    }

    public static ChatRoomResponseDto of(ChatRoom chatRoom, int currentParticipants) {
        return ChatRoomResponseDto.builder()
                .id(chatRoom.getId())
                .title(chatRoom.getTitle())
                .maxCapacity(chatRoom.getMaxCapacity())
                .isAnonymous(chatRoom.isAnonymous())
                .currentParticipants(currentParticipants)
                .createDate(chatRoom.getCreateDate()) // getCreatedAt() 대신 getCreateDate() 사용
                .build();
    }
}
