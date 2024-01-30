package kr.inuappcenterportal.inuportal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MemberUpdateDto {
    @NotBlank
    private String password;

    @Builder
    public MemberUpdateDto(String password){
        this.password = password;
    }
}
