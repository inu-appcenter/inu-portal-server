package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "회원 닉네임 변경 요청Dto")
@Getter
@NoArgsConstructor
public class MemberUpdateNicknameDto {

    @Schema(description = "닉네임",example = "닉네임")
    private String nickname;

    @Schema(description = "횃불이 번호")
    private Long fireId;

    @Builder
    public MemberUpdateNicknameDto(String nickname, Long fireId){
        this.nickname = nickname;
        this.fireId = fireId;
    }
}
