package kr.inuappcenterportal.inuportal.domain.stats.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.stats.dto.AdminStatsResponse;
import kr.inuappcenterportal.inuportal.domain.stats.service.AdminStatsService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/stats")
@Tag(name = "Admin Stats", description = "관리자 통계 대시보드 API")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @Operation(summary = "통계 대시보드 조회",
            description = "[관리자 전용] 기간 내 일별 접속자·가입자, 가입 월별 코호트, API 호출 추이를 조회합니다.<br>" +
                    "데이터 출처(server, mixpanel)별로 status와 data를 따로 담습니다. 한 출처가 실패해도 다른 출처 결과는 응답합니다.<br>" +
                    "기간은 최대 400일이며, 생략하면 오늘까지 최근 30일입니다.")
    @GetMapping
    public ResponseEntity<ResponseDto<AdminStatsResponse>> getStats(
            @Parameter(description = "시작일 (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "종료일 (yyyy-MM-dd). 오늘 이후면 오늘로 맞춥니다")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "API 추이에 포함할 API 수 (기본 15, 최대 50)")
            @RequestParam(required = false) Integer apiLimit) {
        return ResponseEntity.ok(ResponseDto.of(adminStatsService.getStats(from, to, apiLimit), "통계 조회 성공"));
    }
}
