package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Schema(description = "회원정보 수정 요청Dto")
@Getter
@RequiredArgsConstructor
public class MemberUpdateDto {
    @Schema(description = "비밀번호")
    @NotBlank
    private String password;

    @Builder
    public MemberUpdateDto(String password){
        this.password = password;
    }
}
