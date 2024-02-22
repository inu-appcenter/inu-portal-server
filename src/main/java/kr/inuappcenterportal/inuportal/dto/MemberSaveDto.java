package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Schema(description = "회원가입 요청Dto")
@Getter
@NoArgsConstructor
public class MemberSaveDto {
    @Schema(description = "이메일",example = "test@inu.ac.kr")
    @Email
    @NotBlank
    private String email;

    @Schema(description = "비밀번호",example = "12345")
    @NotBlank
    private String password;

    @Schema(description = "닉네임",example = "인천대팁쟁이")
    @NotBlank
    private String nickname;

    @Builder
    public MemberSaveDto(String email, String password, String nickname){
        this.email= email;
        this.password = password;
        this.nickname = nickname;
    }

}
