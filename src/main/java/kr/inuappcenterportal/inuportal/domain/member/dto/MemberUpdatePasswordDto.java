package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Schema(description = "회원 비밀번호 수정 요청Dto")
@Getter
@RequiredArgsConstructor
public class MemberUpdatePasswordDto {

    @Schema(description = "비밀번호",example = "12345")
    @NotBlank
    private String password;

    @Schema(description = "새 비밀번호",example = "1234")
    @NotBlank
    private String newPassword;

    @Builder
    public MemberUpdatePasswordDto(String password, String newPassword){
        this.password = password;
        this.newPassword = newPassword;
    }
}
