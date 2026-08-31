package kr.inuappcenterportal.inuportal.domain.member.dto;

import lombok.Builder;

/**
 * @param friendId friend 엔티티의 ID
 * @param department 친구의 학과명 (한국어)
 */
public record FriendResponseDto(Long friendId, Long friendMemberId, String nickname, String studentId, Long fireId,
                                String friendAlias, String department) {
    @Builder
    public FriendResponseDto {
    }
}
