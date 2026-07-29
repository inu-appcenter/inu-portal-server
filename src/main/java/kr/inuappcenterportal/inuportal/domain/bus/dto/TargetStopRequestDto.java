package kr.inuappcenterportal.inuportal.domain.bus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "수집 대상 정류장 요청 DTO")
public class TargetStopRequestDto {


    @NotBlank(message = "정류소 ID는 필수입니다.")
    @Schema(description = "정류소 ID", example = "165000384")
    private String bstopId;

    @NotBlank(message = "정류소 명칭은 필수입니다.")
    @Schema(description = "정류소 명칭", example = "인천대입구역 2번출구")
    private String bstopName;

    @Schema(description = "정류소 축약명/별칭", example = "인입")
    private String stopAlias;

    @Schema(description = "카테고리", example = "인입런")
    private String category;

}
