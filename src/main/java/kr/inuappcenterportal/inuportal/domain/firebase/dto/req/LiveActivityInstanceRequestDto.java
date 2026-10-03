package kr.inuappcenterportal.inuportal.domain.firebase.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LiveActivityInstanceRequestDto {
    @Schema(description = "이 기기의 FCM 토큰", example = "fcm-token")
    @NotBlank
    private String token;

    @Schema(description = "ActivityKit Activity.id", example = "8538CB42-CBA1-4B26-A953-70E973B675C0")
    @NotBlank
    @Size(max = 64)
    private String activityId;

    @Schema(description = "이 Activity의 ActivityKit 업데이트 push 토큰(hex)", example = "80f2...")
    @NotBlank
    @Size(max = 512)
    private String pushToken;

    @Schema(description = "Activity의 현재 레이아웃 props(JSON 문자열). startTimestamp/endTimestamp(epoch ms)가 있어야 한다.",
            example = "{\"phase\":\"UPCOMING\",\"courseTitle\":\"자료구조\",\"startTimestamp\":1790835307603,\"endTimestamp\":1790839807603}")
    @NotBlank
    @Size(max = 2048)
    private String props;

    @Builder
    public LiveActivityInstanceRequestDto(String token, String activityId, String pushToken, String props) {
        this.token = token;
        this.activityId = activityId;
        this.pushToken = pushToken;
        this.props = props;
    }
}
