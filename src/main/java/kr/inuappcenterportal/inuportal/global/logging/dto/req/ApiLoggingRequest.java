package kr.inuappcenterportal.inuportal.global.logging.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Api 로그 저장 요청")
public record ApiLoggingRequest(

        @Schema(description = "uri", example = "/api/buses")
        @NotBlank(message = "uri가 비어있습니다.")
        @Pattern(regexp = "^/api(/[a-zA-Z0-9_-]+)*$", message = "유효한 uri 형식이 아닙니다. 예시: /api/buses, /api/maps")
        String uri

) {
}
