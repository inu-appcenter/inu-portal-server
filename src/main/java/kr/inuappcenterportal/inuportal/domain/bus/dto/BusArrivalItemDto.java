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
@Schema(description = "버스 도착 정보 DTO")
public class BusArrivalItemDto {

    @Schema(description = "도착 예정 시간 (초)", example = "360")
    private String arrivalEstimateTime;

    @Schema(description = "정류소 ID", example = "165000384")
    private String bstopId;

    @Schema(description = "버스 ID", example = "165000100")
    private String busId;

    @Schema(description = "차량 번호", example = "인천70바1234")
    private String busNumPlate;

    @Schema(description = "혼잡도 (1: 여유, 2: 보통, 3: 혼잡)", example = "1")
    private String congestion;

    @Schema(description = "진행 방향 코드")
    private String dircd;

    @Schema(description = "막차 여부 (Y/N)", example = "N")
    private String lastBusYn;

    @Schema(description = "최근 정류소 ID")
    private String latestStopId;

    @Schema(description = "최근 정류소 명칭", example = "지식정보단지역")
    private String latestStopName;

    @Schema(description = "저상버스 여부")
    private String lowTpCd;

    @Schema(description = "잔여 좌석 수", example = "20")
    private String remaindSeat;

    @Schema(description = "남은 정류장 수", example = "3")
    private String restStopCount;

    @Schema(description = "노선 ID", example = "165000012")
    private String routeId;

    @Schema(description = "노선 번호", example = "8")
    private String routeNo;

    @Schema(description = "통계적 추정 도착 시간 (초, 실시간 미제공 시에만 포함)")
    private Integer estimatedArrivalSeconds;

    @Schema(description = "통계적 추정 문구")
    private String estimationNotice;
}
