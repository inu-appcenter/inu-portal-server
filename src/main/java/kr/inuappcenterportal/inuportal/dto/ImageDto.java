package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Schema(description = "이미지 요청 Dto")
@Getter
@NoArgsConstructor
public class ImageDto {
    @Schema(description = "게시물의 데이터베이스 id값",example = "1")
    @NotNull
    private Long postId;
    @Schema(description = "이미지의 등록 순서",example = "1")
    @NotNull
    private Long imageId;

    @Builder
    public ImageDto (Long postId, Long imageId){
        this.postId = postId;
        this.imageId =imageId;
    }
}
