package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "주변 친구 조회 응답 DTO")
public record NearbyFriendResponseDto(
        @Schema(description = "회원 고유 ID", example = "42")
        Long memberId,

        @Schema(description = "닉네임", example = "인팁이")
        String nickname,

        @Schema(description = "마스킹된 학번", example = "2022***43")
        String studentId,

        @Schema(description = "횃불이 이미지 번호", example = "3")
        Long fireId,

        @Schema(description = "거리(미터)", example = "87")
        Long distanceMeters
) {
    @Builder
    public NearbyFriendResponseDto {
    }
}
