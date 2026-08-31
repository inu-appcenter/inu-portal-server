package kr.inuappcenterportal.inuportal.domain.member.dto;

import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import lombok.Builder;

/**
 * @param friendId friend 엔티티의 ID
 * @param department 친구의 학과
 */
public record FriendResponseDto(Long friendId, Long friendMemberId, String nickname, String studentId, Long fireId,
                                String friendAlias, Department department) {
    @Builder
    public FriendResponseDto {
    }
}
