package kr.inuappcenterportal.inuportal.domain.chat.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ChatRoomMemberResponseDto {
    private String nickname;
    private String studentId;
    private Long fireId;
    private boolean isMe;
    private boolean isOwner;

    @Builder
    public ChatRoomMemberResponseDto(String nickname, String studentId, Long fireId, boolean isMe, boolean isOwner) {
        this.nickname = nickname;
        this.studentId = studentId;
        this.fireId = fireId;
        this.isMe = isMe;
        this.isOwner = isOwner;
    }
}
