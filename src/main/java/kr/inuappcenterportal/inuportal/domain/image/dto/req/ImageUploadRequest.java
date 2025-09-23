package kr.inuappcenterportal.inuportal.domain.image.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "이미지 업로드 요청")
public record ImageUploadRequest(

        @Schema(description = "파일명", example = "Crab")
        @NotBlank(message = "파일명이 비어있습니다.")
        String name,

        @Schema(description = "카테고리", example = "profile-image")
        @NotBlank(message = "카테고리가 비어있습니다.")
        String category

) {
}