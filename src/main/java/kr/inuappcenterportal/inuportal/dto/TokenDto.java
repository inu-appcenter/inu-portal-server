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

    @Schema(description = "엑세스 토큰 만료 시간")
    private String accessTokenExpiredTime;

    @Schema(description = "리프레시 토큰")
    private String refreshToken;

    @Schema(description = "리프레쉬 토큰 만료 시간")
    private String refreshTokenExpiredTime;



    @Builder
    private TokenDto(String accessToken,String refreshToken, String accessTokenExpiredTime, String refreshTokenExpiredTime){
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiredTime = accessTokenExpiredTime;
        this.refreshTokenExpiredTime = refreshTokenExpiredTime;
    }
    public static TokenDto of(String accessToken, String refreshToken, String accessTokenExpiredTime, String refreshTokenExpiredTime){
        return TokenDto.builder().accessToken(accessToken).refreshToken(refreshToken).accessTokenExpiredTime(accessTokenExpiredTime).refreshTokenExpiredTime(refreshTokenExpiredTime).build();
    }

}
