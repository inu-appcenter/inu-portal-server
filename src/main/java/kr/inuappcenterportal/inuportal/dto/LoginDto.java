package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "로그인 요청Dto")
@Getter
@NoArgsConstructor
public class LoginDto {
    @Schema(description = "학번",example = "201901591")
    @NotBlank
    private String num;

    @Schema(description = "비밀번호",example = "12345")
    @NotBlank
    private String password;

    @Builder
    public LoginDto(String num, String password){
        this.num = num;
        this.password = password;
    }
}
