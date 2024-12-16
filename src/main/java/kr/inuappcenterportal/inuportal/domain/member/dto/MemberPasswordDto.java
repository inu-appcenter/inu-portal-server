package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "회원의 비밀번호 일치여부 확인을 위한 Dto")
@Getter
@NoArgsConstructor
public class MemberPasswordDto {
    @Schema(description = "비밀번호",example = "12345")
    @NotBlank
    private String password;

    @Builder
    public MemberPasswordDto(String password){
        this.password = password;
    }
}
