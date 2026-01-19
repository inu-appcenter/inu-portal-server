package kr.inuappcenterportal.inuportal.domain.image.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이미지 처리 응답")
public record ImageResponse(

        @Schema(description = "파일 url")
        String url

) {
    public static ImageResponse from(String url) {
        return new ImageResponse(url);
    }
}
