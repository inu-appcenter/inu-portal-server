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

    @Operation(summary = "학교/하교 전체 노선 자동 탐색 및 동기화", description = "주요 출발 정류장을 통과하는 버스 노선을 자동 탐색하고 학교 캠퍼스 최장 종점까지 자동 슬라이싱하여 노선 구간을 구축합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "동기화 성공")
    })
    @PostMapping("/routes/auto-sync")
    public ResponseEntity<ResponseDto<List<BusRouteSectionResponseDto>>> autoSyncRoutes() {
        List<BusRouteSectionResponseDto> result = busService.autoSyncRoutes();
        return ResponseEntity.ok(ResponseDto.of(result, String.format("총 %d개의 노선 구간이 자동 탐색 및 동기화되었습니다.", result.size())));
    }

    @Operation(summary = "등록된 동적 노선 구간 목록 조회", description = "어드민에 등록된 모든 노선 구간 및 정류장 목록을 조회합니다.")

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/routes")
    public ResponseEntity<ResponseDto<List<BusRouteSectionResponseDto>>> getRouteSections() {
        List<BusRouteSectionResponseDto> result = busService.getRouteSections(null);
        return ResponseEntity.ok(ResponseDto.of(result, "등록된 노선 구간 목록 조회 성공"));
    }

    @Operation(summary = "등록된 동적 노선 구간 정보 직접 수정", description = "어드민이 노선의 구간명, 탭명, 카테고리, 운행안내, 한 줄 팁을 직접 수정합니다.")
    @PutMapping("/routes/{id}")
    public ResponseEntity<ResponseDto<BusRouteSectionResponseDto>> updateRouteSection(
            @PathVariable Long id,
            @Valid @RequestBody kr.inuappcenterportal.inuportal.domain.bus.dto.RouteSectionUpdateRequest request) {
        BusRouteSectionResponseDto result = busService.updateRouteSection(id, request);
        return ResponseEntity.ok(ResponseDto.of(result, "노선 구간 정보가 수정되었습니다."));
    }

    @Operation(summary = "동적 노선 구간 삭제", description = "등록된 노선 구간을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공")
    })
    @DeleteMapping("/routes/{id}")
    public ResponseEntity<ResponseDto<Void>> deleteRouteSection(@PathVariable Long id) {
        busService.deleteRouteSection(id);
        return ResponseEntity.ok(ResponseDto.of(null, "노선 구간 삭제 성공"));
    }

    @Operation(summary = "공공데이터 정류소 실시간 검색", description = "키워드로 인천 버스 정류소를 검색하고 등록된 별칭 정보와 함께 반환합니다.")
    @GetMapping("/stops/search")
    public ResponseEntity<ResponseDto<List<kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopSearchDto>>> searchBusStops(
            @RequestParam String keyword) {
        List<kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopSearchDto> result = busService.searchBusStops(keyword);
        return ResponseEntity.ok(ResponseDto.of(result, "정류소 검색 성공"));
    }

    @Operation(summary = "정류소 별칭 목록 조회", description = "등록된 모든 정류소 별칭 사전을 조회합니다.")
    @GetMapping("/stop-aliases")
    public ResponseEntity<ResponseDto<List<kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopAliasDto>>> getStopAliases() {
        List<kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopAliasDto> result = busService.getStopAliases();
        return ResponseEntity.ok(ResponseDto.of(result, "정류소 별칭 목록 조회 성공"));
    }

    @Operation(summary = "정류소 별칭 등록 및 수정", description = "정류소 ID에 대한 축약명/별칭을 등록하거나 수정합니다.")
    @PostMapping("/stop-aliases")
    public ResponseEntity<ResponseDto<kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopAliasDto>> saveStopAlias(
            @Valid @RequestBody kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopAliasDto request) {
        kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopAliasDto result = busService.saveStopAlias(request);
        return ResponseEntity.ok(ResponseDto.of(result, "정류소 별칭 저장 성공"));
    }

    @Operation(summary = "정류소 별칭 삭제", description = "등록된 정류소 별칭을 삭제합니다.")
    @DeleteMapping("/stop-aliases/{id}")
    public ResponseEntity<ResponseDto<Void>> deleteStopAlias(@PathVariable Long id) {
        busService.deleteStopAlias(id);
        return ResponseEntity.ok(ResponseDto.of(null, "정류소 별칭 삭제 성공"));
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

    @Operation(summary = "자동 탐색 타겟 규칙 목록 조회", description = "자동 탐색 및 슬라이싱 시 사용되는 시종점 및 목적지 키워드 규칙 목록을 조회합니다.")
    @GetMapping("/target-rules")
    public ResponseEntity<ResponseDto<List<kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule>>> getTargetRules() {
        List<kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule> result = busService.getTargetRules();
        return ResponseEntity.ok(ResponseDto.of(result, "타겟 규칙 목록 조회 성공"));
    }

    @Operation(summary = "자동 탐색 타겟 규칙 추가", description = "카테고리, 탭명, 시작 정류소, 목적지 키워드를 포함하는 탐색 규칙을 추가합니다.")
    @PostMapping("/target-rules")
    public ResponseEntity<ResponseDto<kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule>> addTargetRule(
            @Valid @RequestBody kr.inuappcenterportal.inuportal.domain.bus.dto.BusTargetRuleDto request) {
        kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule result = busService.addTargetRule(request);
        return ResponseEntity.ok(ResponseDto.of(result, "타겟 규칙 추가 성공"));
    }

    @Operation(summary = "자동 탐색 타겟 규칙 수정", description = "기존 등록된 탐색 규칙을 수정합니다.")
    @PutMapping("/target-rules/{id}")
    public ResponseEntity<ResponseDto<kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule>> updateTargetRule(
            @PathVariable Long id,
            @Valid @RequestBody kr.inuappcenterportal.inuportal.domain.bus.dto.BusTargetRuleDto request) {
        kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule result = busService.updateTargetRule(id, request);
        return ResponseEntity.ok(ResponseDto.of(result, "타겟 규칙 수정 성공"));
    }

    @Operation(summary = "자동 탐색 타겟 규칙 삭제", description = "등록된 탐색 규칙을 삭제합니다.")
    @DeleteMapping("/target-rules/{id}")
    public ResponseEntity<ResponseDto<Void>> deleteTargetRule(@PathVariable Long id) {
        busService.deleteTargetRule(id);
        return ResponseEntity.ok(ResponseDto.of(null, "타겟 규칙 삭제 성공"));
    }

    @Operation(summary = "오래된 버스 도착 기록 수동 정리", description = "기본 4주(28일) 또는 지정한 주(weeks) 이전의 과거 버스 도착 기록을 즉시 삭제합니다.")
    @PostMapping("/history/cleanup")
    public ResponseEntity<ResponseDto<Integer>> cleanupHistory(
            @RequestParam(defaultValue = "4") int weeks) {
        int deleted = busService.cleanupOldArrivalHistory(weeks);
        return ResponseEntity.ok(ResponseDto.of(deleted, String.format("%d주 이전 버스 도착 기록 %d건이 정리되었습니다.", weeks, deleted)));
    }

    @Operation(summary = "버스 실시간 수집 및 서비스 동작 상태 조회", description = "DB에 영구 저장된 버스 서비스 및 30초 실시간 도착 정보 수집 활성화 여부를 조회합니다.")
    @GetMapping("/service-status")
    public ResponseEntity<ResponseDto<Boolean>> getServiceStatus() {
        boolean enabled = busService.isBusServiceEnabled();
        return ResponseEntity.ok(ResponseDto.of(enabled, "버스 서비스 상태 조회 성공"));
    }

    @Operation(summary = "버스 실시간 수집 및 서비스 동작 상태 변경 (ON/OFF)", description = "DB에 버스 서비스 활성화/비활성화 상태를 저장하여 서버 재기동 시에도 영구 유지합니다.")
    @PostMapping("/service-status")
    public ResponseEntity<ResponseDto<Boolean>> updateServiceStatus(@RequestParam boolean enabled) {
        boolean result = busService.updateBusServiceStatus(enabled);
        String msg = result ? "버스 실시간 수집 서비스가 활성화(ON)되었습니다." : "버스 실시간 수집 서비스가 비활성화(OFF)되었습니다.";
        return ResponseEntity.ok(ResponseDto.of(result, msg));
    }
}



