package kr.inuappcenterportal.inuportal.global.logging.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "API 로그 응답")
public record LoggingApiResponse(

        @Schema(description = "호출된 uri")
        String uri,

        @Schema(description = "호출된 횟수")
        Long apiCount

) {
    public static LoggingApiResponse of(String uri, Long apiCount) {
        return new LoggingApiResponse(uri, apiCount);
    }
}
