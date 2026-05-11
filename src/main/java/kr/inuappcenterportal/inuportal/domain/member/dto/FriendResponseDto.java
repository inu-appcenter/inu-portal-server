package kr.inuappcenterportal.inuportal.domain.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class FriendResponseDto {
    private Long friendId; // friend 엔티티의 ID
    private Long memberId; // 상대방 member ID
    private String nickname;
    private String studentId;
    private Long fireId;

    @Builder
    public FriendResponseDto(Long friendId, Long memberId, String nickname, String studentId, Long fireId) {
        this.friendId = friendId;
        this.memberId = memberId;
        this.nickname = nickname;
        this.studentId = studentId;
        this.fireId = fireId;
    }
}
