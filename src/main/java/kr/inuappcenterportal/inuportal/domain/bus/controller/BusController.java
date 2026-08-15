package kr.inuappcenterportal.inuportal.domain.bus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusArrivalItemDto;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusHistoryResponseDto;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusRouteSectionResponseDto;
import kr.inuappcenterportal.inuportal.domain.bus.service.BusService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Buses", description = "버스 정보 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/buses")
public class BusController {

    private final BusService busService;

    @Operation(summary = "실시간 버스 도착 정보 조회", description = "정류소 ID(bstopId) 기반 실시간 버스 도착 정보를 제공하며, 데이터 부재 시 통계적 추정치를 포함합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/arrivals")
    public ResponseEntity<ResponseDto<List<BusArrivalItemDto>>> getBusArrivals(
            @Parameter(description = "정류소 ID", example = "165000384") @RequestParam String bstopId) {
        List<BusArrivalItemDto> arrivals = busService.getRealtimeArrivals(bstopId);
        return ResponseEntity.ok(ResponseDto.of(arrivals, "실시간 버스 도착 정보 조회 성공"));
    }

    @Operation(summary = "과거 버스 도착 이력 및 시간표 조회", description = "특정 정류장의 지정한 날짜(YYYY-MM-DD) 과거 도착 이력 및 동 요일 평균 도착 소요시간 통계를 제공합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/history")
    public ResponseEntity<ResponseDto<BusHistoryResponseDto>> getBusHistory(
            @Parameter(description = "정류소 ID", example = "165000384") @RequestParam String bstopId,
            @Parameter(description = "조회 대상 날짜 (YYYY-MM-DD)", example = "2026-07-29") @RequestParam(required = false) String targetDate) {
        BusHistoryResponseDto history = busService.getHistory(bstopId, targetDate);
        return ResponseEntity.ok(ResponseDto.of(history, "과거 버스 도착 이력 조회 성공"));
    }

    @Operation(summary = "동적 버스 노선 구간 및 정류장 목록 조회", description = "카테고리(go-school, go-home, shuttle 등)에 맞는 노선 구간 및 경유 정류장 좌표를 제공합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/routes")
    public ResponseEntity<ResponseDto<List<BusRouteSectionResponseDto>>> getRouteSections(
            @Parameter(description = "카테고리 (go-school, go-home, shuttle)", example = "go-school") @RequestParam(required = false) String category) {
        List<BusRouteSectionResponseDto> sections = busService.getRouteSections(category);
        return ResponseEntity.ok(ResponseDto.of(sections, "동적 노선 구간 조회 성공"));
    }

    @Operation(summary = "정류소 별칭 및 안내 문구 목록 조회", description = "정류소별 짧은 별칭 및 실시간 혼잡도/이용 팁 안내 문구를 제공합니다.")
    @GetMapping("/stop-aliases")
    public ResponseEntity<ResponseDto<List<kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopAliasDto>>> getStopAliases() {
        List<kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopAliasDto> aliases = busService.getStopAliases();
        return ResponseEntity.ok(ResponseDto.of(aliases, "정류소 별칭 및 안내 문구 목록 조회 성공"));
    }
}

