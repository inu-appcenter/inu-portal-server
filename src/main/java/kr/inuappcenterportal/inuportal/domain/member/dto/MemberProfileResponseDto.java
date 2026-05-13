package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "유저 프로필 응답 DTO")
public class MemberProfileResponseDto {
    @Schema(description = "유저 ID")
    private Long memberId;
    @Schema(description = "닉네임")
    private String nickname;
    @Schema(description = "프로필 이미지 번호")
    private Long fireId;
    @Schema(description = "학과")
    private Department department;
    @Schema(description = "마스킹된 학번")
    private String maskedStudentId;
    @Schema(description = "친구 상태 (NONE, PENDING, RECEIVED, ACCEPTED)")
    private String friendStatus;
    @Schema(description = "친구 데이터 ID (친구 삭제 시 필요)")
    private Long friendId;
}
