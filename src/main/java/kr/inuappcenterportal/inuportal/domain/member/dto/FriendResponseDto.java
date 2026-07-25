package kr.inuappcenterportal.inuportal.domain.member.dto;

import lombok.Builder;

/**
 * @param friendId friend 엔티티의 ID
 */
public record FriendResponseDto(Long friendId, Long friendMemberId, String nickname, String studentId, Long fireId,
                                String friendAlias) {
    @Builder
    public FriendResponseDto {
    }
}
