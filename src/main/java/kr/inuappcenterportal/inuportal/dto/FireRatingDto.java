package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "횃불이 ai 그림 평점 등록 요청Dto")
@NoArgsConstructor
@Getter
public class FireRatingDto {

    private Long u_id;

    @Schema(description = "그림의 요청번호",example = "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6")
    @NotBlank
    private String req_id;

    @Schema(description = "평점(0~5)")
    @Min(0)
    @Max(5)
    private Integer rating;

    @Schema(description = "한 줄평",example = "손이 어색해요")
    private String comment;

    public void setU_id(Long u_id) {
        this.u_id = u_id;
    }
}
