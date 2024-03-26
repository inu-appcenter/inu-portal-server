package kr.inuappcenterportal.inuportal.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Schema(description = "토큰 응답 Dto")
@Getter
@NoArgsConstructor
public class TokenDto {
    @Schema(description = "액세스 토큰")
    private String accessToken;

    @Schema(description = "리프레시 토큰")
    private String refreshToken;

    @Builder
    private TokenDto(String accessToken,String refreshToken){
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
    public static TokenDto of(String accessToken, String refreshToken){
        return TokenDto.builder().accessToken(accessToken).refreshToken(refreshToken).build();
    }

}
