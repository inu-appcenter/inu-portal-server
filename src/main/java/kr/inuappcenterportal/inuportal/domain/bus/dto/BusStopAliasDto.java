package kr.inuappcenterportal.inuportal.domain.bus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "정류소 별칭 사전 DTO")
public class BusStopAliasDto {

    private Long id;

    @NotBlank(message = "정류소 ID는 필수입니다.")
    @Schema(description = "공공데이터 정류소 ID", example = "164000395")
    private String bstopId;

    @NotBlank(message = "정류소 명칭은 필수입니다.")
    @Schema(description = "정류소 공식 명칭", example = "인천대입구역 2번출구")
    private String bstopName;

    @NotBlank(message = "별칭/축약명은 필수입니다.")
    @Schema(description = "정류소 별칭/축약명", example = "인입")
    private String stopAlias;

    @Schema(description = "정류장 상단 안내 문구", example = "※ 8시 ~ 10시에는 매우 혼잡해요. 계단에서 줄서기를 꼭 지켜주세요.")
    private String stopNotice;

    @Schema(description = "메모", example = "인입런 2번출구 정류소")
    private String memo;
}

