package kr.inuappcenterportal.inuportal.domain.bus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusRouteSectionResponseDto;
import kr.inuappcenterportal.inuportal.domain.bus.dto.RouteSectionCreateRequest;
import kr.inuappcenterportal.inuportal.domain.bus.dto.TargetStopRequestDto;
import kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetStop;
import kr.inuappcenterportal.inuportal.domain.bus.service.BusService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Admin Buses", description = "어드민 버스 노선 및 정류장 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/buses")
public class AdminBusController {

    private final BusService busService;

    @Operation(summary = "동적 노선 구간 등록 및 정류장 슬라이싱 생성", description = "노선 번호, 기점 및 종점 정류장을 지정하여 해당 구간의 정류장 목록 및 좌표를 자동 슬라이싱 후 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 성공")
    })
    @PostMapping("/routes")
    public ResponseEntity<ResponseDto<BusRouteSectionResponseDto>> createRouteSection(
            @Valid @RequestBody RouteSectionCreateRequest request) {
        BusRouteSectionResponseDto result = busService.createOrUpdateRouteSection(request);
        return ResponseEntity.ok(ResponseDto.of(result, "노선 구간 등록 성공"));
    }

    @Operation(summary = "30초 폴링 수집 대상 정류장 추가", description = "30초 간격으로 실시간 도착 정보를 수집할 기점/출발 정류장을 추가합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추가 성공")
    })
    @PostMapping("/target-stops")
    public ResponseEntity<ResponseDto<BusTargetStop>> addTargetStop(
            @Valid @RequestBody TargetStopRequestDto request) {
        BusTargetStop result = busService.addTargetStop(request);
        return ResponseEntity.ok(ResponseDto.of(result, "수집 대상 정류장 추가 성공"));
    }

    @Operation(summary = "30초 폴링 수집 대상 정류장 목록 조회", description = "현재 폴링 중인 수집 대상 정류장 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/target-stops")
    public ResponseEntity<ResponseDto<List<BusTargetStop>>> getTargetStops() {
        List<BusTargetStop> result = busService.getTargetStops();
        return ResponseEntity.ok(ResponseDto.of(result, "수집 대상 정류장 목록 조회 성공"));
    }
}
