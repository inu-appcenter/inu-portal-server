package kr.inuappcenterportal.inuportal.global.logging.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Api 로그 저장 요청")
public record ApiLoggingRequest(

        @Schema(description = "uri", example = "/api/buses")
        @NotBlank(message = "uri가 비어있습니다.")
        String uri

) {
}
