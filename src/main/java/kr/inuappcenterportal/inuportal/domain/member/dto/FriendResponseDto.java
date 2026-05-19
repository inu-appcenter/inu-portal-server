package kr.inuappcenterportal.inuportal.domain.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class FriendResponseDto {
    private Long friendId; // friend 엔티티의 ID
    private String nickname;
    private String studentId;
    private Long fireId;
    private String friendAlias;

    @Builder
    public FriendResponseDto(Long friendId, String nickname, String studentId, Long fireId, String friendAlias) {
        this.friendId = friendId;
        this.nickname = nickname;
        this.studentId = studentId;
        this.fireId = fireId;
        this.friendAlias = friendAlias;
    }
}
