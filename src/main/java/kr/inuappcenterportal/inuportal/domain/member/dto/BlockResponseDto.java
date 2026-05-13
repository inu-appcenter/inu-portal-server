package kr.inuappcenterportal.inuportal.domain.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class BlockResponseDto {
    private Long blockId;
    private Long blockedMemberId;
    private String nickname;
    private String studentId;

    @Builder
    public BlockResponseDto(Long blockId, Long blockedMemberId, String nickname, String studentId) {
        this.blockId = blockId;
        this.blockedMemberId = blockedMemberId;
        this.nickname = nickname;
        this.studentId = studentId;
    }
}
