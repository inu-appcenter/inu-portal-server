package kr.inuappcenterportal.inuportal.domain.member.dto;

import lombok.Builder;

/**
 * 초대 링크를 연 사람에게 "누구의 링크인지" 보여주기 위한 미리보기.
 * 비로그인 상태에서도 조회되므로 학번은 마스킹된 값만, 회원 식별자는 담지 않는다.
 */
public record FriendInvitePreviewResponseDto(String nickname, String studentId, Long fireId) {
    @Builder
    public FriendInvitePreviewResponseDto {
    }
}
