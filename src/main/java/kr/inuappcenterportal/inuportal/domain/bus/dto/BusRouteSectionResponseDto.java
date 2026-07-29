package kr.inuappcenterportal.inuportal.domain.bus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.bus.entity.BusRouteSection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "동적 노선 구간 응답 DTO")
public class BusRouteSectionResponseDto {

    @Schema(description = "구간 ID", example = "1")
    private Long id;

    @Schema(description = "구간명", example = "인입런 - 8번")
    private String sectionName;

    @Schema(description = "카테고리 (go-school, go-home, shuttle)", example = "go-school")
    private String category;

    @Schema(description = "탭명", example = "인입런")
    private String tabName;

    @Schema(description = "노선 번호", example = "8")
    private String routeNo;

    @Schema(description = "노선 ID", example = "165000012")
    private String routeId;

    @Schema(description = "기점 정류장 ID", example = "165000384")
    private String startBstopId;

    @Schema(description = "기점 정류장 명칭", example = "인천대입구역 2번출구")
    private String startBstopName;

    @Schema(description = "종점 정류장 ID", example = "165000350")
    private String endBstopId;

    @Schema(description = "종점 정류장 명칭", example = "인천대학교 자연과학대학")
    private String endBstopName;

    @Schema(description = "경유 정류장 및 좌표 목록")
    private List<BusRouteStopDto> stops;

    public static BusRouteSectionResponseDto from(BusRouteSection section) {
        return BusRouteSectionResponseDto.builder()
                .id(section.getId())
                .sectionName(section.getSectionName())
                .category(section.getCategory())
                .tabName(section.getTabName())
                .routeNo(section.getRouteNo())
                .routeId(section.getRouteId())
                .startBstopId(section.getStartBstopId())
                .startBstopName(section.getStartBstopName())
                .endBstopId(section.getEndBstopId())
                .endBstopName(section.getEndBstopName())
                .stops(section.getStops().stream()
                        .map(stop -> BusRouteStopDto.builder()
                                .seq(stop.getSeq())
                                .bstopId(stop.getBstopId())
                                .bstopName(stop.getBstopName())
                                .latitude(stop.getLatitude())
                                .longitude(stop.getLongitude())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
