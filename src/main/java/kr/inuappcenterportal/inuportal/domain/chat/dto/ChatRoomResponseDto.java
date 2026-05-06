package kr.inuappcenterportal.inuportal.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoom;
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
    private int maxCapacity;
    private boolean isAnonymous;
    private int currentParticipants;
    private LocalDateTime createDate;
    private String myHash; // 본인 확인용 해시
    private List<ChatMessageResponseDto> messages; // 메시지 목록

    @Builder
    public ChatRoomResponseDto(Long id, String title, int maxCapacity, boolean isAnonymous, int currentParticipants, LocalDateTime createDate, String myHash, List<ChatMessageResponseDto> messages) {
        this.id = id;
        this.title = title;
        this.maxCapacity = maxCapacity;
        this.isAnonymous = isAnonymous;
        this.currentParticipants = currentParticipants;
        this.createDate = createDate;
        this.myHash = myHash;
        this.messages = messages;
    }

    public static ChatRoomResponseDto of(ChatRoom chatRoom, int currentParticipants) {
        return ChatRoomResponseDto.builder()
                .id(chatRoom.getId())
                .title(chatRoom.getTitle())
                .maxCapacity(chatRoom.getMaxCapacity())
                .isAnonymous(chatRoom.isAnonymous())
                .currentParticipants(currentParticipants)
                .createDate(chatRoom.getCreateDate())
                .build();
    }

    public static ChatRoomResponseDto of(ChatRoom chatRoom, int currentParticipants, String myHash) {
        return ChatRoomResponseDto.builder()
                .id(chatRoom.getId())
                .title(chatRoom.getTitle())
                .maxCapacity(chatRoom.getMaxCapacity())
                .isAnonymous(chatRoom.isAnonymous())
                .currentParticipants(currentParticipants)
                .createDate(chatRoom.getCreateDate())
                .myHash(myHash)
                .build();
    }

    public static ChatRoomResponseDto of(ChatRoom chatRoom, int currentParticipants, String myHash, List<ChatMessageResponseDto> messages) {
        return ChatRoomResponseDto.builder()
                .id(chatRoom.getId())
                .title(chatRoom.getTitle())
                .maxCapacity(chatRoom.getMaxCapacity())
                .isAnonymous(chatRoom.isAnonymous())
                .currentParticipants(currentParticipants)
                .createDate(chatRoom.getCreateDate())
                .myHash(myHash)
                .messages(messages)
                .build();
    }
}
