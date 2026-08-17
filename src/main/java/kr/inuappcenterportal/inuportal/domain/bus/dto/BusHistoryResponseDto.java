package kr.inuappcenterportal.inuportal.domain.bus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "과거 버스 도착 이력 및 비교 DTO")
public class BusHistoryResponseDto {

    @Schema(description = "정류소 ID", example = "165000384")
    private String bstopId;

    @Schema(description = "조회 날짜 (YYYY-MM-DD)", example = "2026-07-29")
    private String targetDate;

    @Schema(description = "요일 (MONDAY ~ SUNDAY)", example = "WEDNESDAY")
    private String dayOfWeek;

    @Schema(description = "도착 이력 목록")
    private List<HistoryRecord> historyRecords;

    @Schema(description = "동일 요일 최근 4주 평균/중앙값 도착 소요 시간 (초)")
    private Integer averageIntervalSeconds;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryRecord {
        private Long id;
        private String routeId;
        private String routeNo;
        private String busNumPlate;
        private Integer arrivalEstimateTime;
        private Integer restStopCount;
        private LocalDateTime arrivalTime;
    }
}
