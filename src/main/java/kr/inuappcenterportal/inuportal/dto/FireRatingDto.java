package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "횃불이 ai 그림 별점 부여 요청Dto")
@NoArgsConstructor
@Getter
public class FireRatingDto {
    @Schema(description = "횃불이 ai 이미지 평점")
    @NotNull
    @Min(value = 1, message = "평점은 1~5이어야 합니다.")
    @Max(value = 5, message = "평점은 1~5이여야 합니다.")
    private Integer rating;

    @Builder
    public FireRatingDto(Integer rating){
        this.rating=rating;
    }
}
