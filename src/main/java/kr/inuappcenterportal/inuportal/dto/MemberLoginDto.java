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
public class MemberLoginDto {

    @Schema(description = "이메일",example = "test@inu.ac.kr")
    @Email
    @NotBlank
    private String email;

    @Schema(description = "비밀번호",example = "12345")
    @NotBlank
    private String password;

    @Builder
    public MemberLoginDto(String email, String password){
        this.email = email;
        this.password = password;
    }
}
