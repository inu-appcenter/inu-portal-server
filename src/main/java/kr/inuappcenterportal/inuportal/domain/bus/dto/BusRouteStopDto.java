package kr.inuappcenterportal.inuportal.domain.bus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "노선 경유 정류장 DTO")
public class BusRouteStopDto {

    @Schema(description = "순번", example = "1")
    private Integer seq;

    @Schema(description = "정류소 ID", example = "165000384")
    private String bstopId;

    @Schema(description = "정류소 명칭", example = "인천대입구역 2번출구")
    private String bstopName;

    @Schema(description = "위도 (Latitude)", example = "37.3847")
    private Double latitude;

    @Schema(description = "경도 (Longitude)", example = "126.6432")
    private Double longitude;
}
