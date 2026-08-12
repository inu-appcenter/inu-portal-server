package kr.inuappcenterportal.inuportal.domain.suggestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "건의사항 등록 Dto")
@Getter
@NoArgsConstructor
public class SuggestionRequest {

    @Schema(description = "건의 내용", example = "특정 게시판에서 이미지 업로드가 안 돼요.")
    @NotBlank
    @Size(max = 2000)
    private String content;

    @Schema(description = "개발자 응원 메시지", example = "항상 잘 쓰고 있어요, 감사합니다!")
    @Size(max = 1000)
    private String cheerMessage;

    @Schema(description = "문의 유형", example = "BUG", allowableValues = {"BUG", "FEATURE_REQUEST", "ETC"})
    @NotBlank
    private String category;

    @Schema(description = "앱 버전 (클라이언트가 자동으로 채워서 전송)", example = "1.4.2")
    private String appVersion;

    @Schema(description = "OS 타입 (클라이언트가 자동으로 채워서 전송)", example = "IOS")
    private String osType;

    @Schema(description = "OS 버전 (클라이언트가 자동으로 채워서 전송)", example = "17.4")
    private String osVersion;

    @Schema(description = "기기 모델명 (클라이언트가 자동으로 채워서 전송)", example = "iPhone15,3")
    private String deviceModel;

    @Builder
    public SuggestionRequest(String content, String cheerMessage, String category, String appVersion,
                              String osType, String osVersion, String deviceModel) {
        this.content = content;
        this.cheerMessage = cheerMessage;
        this.category = category;
        this.appVersion = appVersion;
        this.osType = osType;
        this.osVersion = osVersion;
        this.deviceModel = deviceModel;
    }
}
