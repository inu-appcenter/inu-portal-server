package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "이메일 인증번호 전송 요청Dto")
@NoArgsConstructor
@Getter
public class EmailCheckDto {
    @Schema(description = "이메일", example = "aaa@inu.ac.kr")
    @Email
    @NotBlank
    private String email;

    @Schema(description = "인증번호", example = "123456")
    @NotBlank
    private String numbers;

    @Builder
    public EmailCheckDto(String email, String numbers){
        this.email = email;
        this.numbers = numbers;
    }
}
