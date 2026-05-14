package kr.inuappcenterportal.inuportal.domain.chat.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ChatRoomMemberResponseDto {
    private String nickname;
    private Long memberId;
    private String studentId;
    private Long fireId;
    private boolean isMe;
    private boolean isOwner;
    private String friendAlias;

    @Builder
    public ChatRoomMemberResponseDto(String nickname, Long memberId, String studentId, Long fireId, boolean isMe, boolean isOwner, String friendAlias) {
        this.nickname = nickname;
        this.memberId = memberId;
        this.studentId = studentId;
        this.fireId = fireId;
        this.isMe = isMe;
        this.isOwner = isOwner;
        this.friendAlias = friendAlias;
    }
}
