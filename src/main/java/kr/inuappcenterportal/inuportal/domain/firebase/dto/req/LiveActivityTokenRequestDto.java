package kr.inuappcenterportal.inuportal.domain.firebase.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LiveActivityTokenRequestDto {
    @Schema(description = "이 기기의 FCM 토큰", example = "fcm-token")
    @NotBlank
    private String token;

    @Schema(description = "ActivityKit push-to-start 토큰(hex). null이나 빈 값이면 등록을 해제합니다.", example = "80f2...")
    private String liveActivityStartToken;

    @Builder
    public LiveActivityTokenRequestDto(String token, String liveActivityStartToken) {
        this.token = token;
        this.liveActivityStartToken = liveActivityStartToken;
    }
}
