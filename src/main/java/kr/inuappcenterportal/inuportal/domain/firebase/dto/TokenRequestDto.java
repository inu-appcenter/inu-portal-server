package kr.inuappcenterportal.inuportal.domain.firebase.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenRequestDto {
    @NotBlank
    private String token;

    @Builder
    public TokenRequestDto(String token){
        this.token = token;
    }
}
