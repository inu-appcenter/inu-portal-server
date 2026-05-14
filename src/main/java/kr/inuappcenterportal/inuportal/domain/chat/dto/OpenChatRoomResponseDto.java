package kr.inuappcenterportal.inuportal.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoom;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "오픈채팅방 목록 응답 DTO")
public class OpenChatRoomResponseDto {
    @Schema(description = "채팅방 ID")
    private Long roomId;
    @Schema(description = "채팅방 제목")
    private String title;
    @Schema(description = "채팅방 설명")
    private String description;
    @Schema(description = "채팅방 썸네일")
    private String thumbnailUrl;
    @Schema(description = "방장 닉네임")
    private String ownerNickname;
    @Schema(description = "최대 인원")
    private int maxCapacity;
    @Schema(description = "현재 인원")
    private int currentParticipants;
    @Schema(description = "익명 여부")
    private boolean isAnonymous;
    @Schema(description = "참여 여부")
    private boolean isJoined;
    @Schema(description = "생성일")
    private LocalDateTime createDate;
    @Schema(description = "공식 여부")
    private boolean isOfficial;

    public static OpenChatRoomResponseDto of(ChatRoom room, int currentParticipants, String ownerNickname, boolean isJoined) {
        return OpenChatRoomResponseDto.builder()
                .roomId(room.getId())
                .title(room.getTitle())
                .description(room.getDescription())
                .thumbnailUrl(room.getThumbnailUrl())
                .ownerNickname(ownerNickname)
                .maxCapacity(room.getMaxCapacity())
                .currentParticipants(currentParticipants)
                .isAnonymous(room.isAnonymous())
                .isJoined(isJoined)
                .createDate(room.getCreateDate())
                .isOfficial(room.isOfficial())
                .build();
    }
}
