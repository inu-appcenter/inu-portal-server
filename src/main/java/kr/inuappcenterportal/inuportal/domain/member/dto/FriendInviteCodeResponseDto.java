package kr.inuappcenterportal.inuportal.domain.member.dto;

import lombok.Builder;

/**
 * 내 친구추가 초대 코드.
 *
 * @param code 초대 코드(난수). 클라이언트가 배포 환경에 맞는 origin 으로 링크를 조립할 때 쓴다.
 * @param url  서버 설정 기준으로 조립한 초대 URL. 공유 문구 등 서버 발신 채널에서 쓰는 정규 링크.
 */
public record FriendInviteCodeResponseDto(String code, String url) {
    @Builder
    public FriendInviteCodeResponseDto {
    }
}
