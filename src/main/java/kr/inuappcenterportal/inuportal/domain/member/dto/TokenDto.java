package kr.inuappcenterportal.inuportal.domain.member.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
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

    @Schema(description = "이번 로그인 요청에서 회원이 새로 생성되었는지 여부(첫 로그인=가입). 토큰 재발급 응답에서는 항상 false", example = "false")
    @JsonProperty("isNewMember")
    private boolean newMember;

    @Builder
    private TokenDto(String accessToken,String refreshToken, String accessTokenExpiredTime, String refreshTokenExpiredTime, boolean newMember){
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiredTime = accessTokenExpiredTime;
        this.refreshTokenExpiredTime = refreshTokenExpiredTime;
        this.newMember = newMember;
    }
    public static TokenDto of(String accessToken, String refreshToken, String accessTokenExpiredTime, String refreshTokenExpiredTime){
        return TokenDto.builder().accessToken(accessToken).refreshToken(refreshToken).accessTokenExpiredTime(accessTokenExpiredTime).refreshTokenExpiredTime(refreshTokenExpiredTime).build();
    }

    public static TokenDto of(String accessToken, String refreshToken, String accessTokenExpiredTime, String refreshTokenExpiredTime, boolean newMember){
        return TokenDto.builder().accessToken(accessToken).refreshToken(refreshToken).accessTokenExpiredTime(accessTokenExpiredTime).refreshTokenExpiredTime(refreshTokenExpiredTime).newMember(newMember).build();
    }

}
