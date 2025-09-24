package kr.inuappcenterportal.inuportal.global.logging.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.global.logging.dto.req.ApiLoggingRequest;
import kr.inuappcenterportal.inuportal.global.logging.dto.res.LoggingApiResponse;
import kr.inuappcenterportal.inuportal.global.logging.dto.res.LoggingMemberResponse;
import kr.inuappcenterportal.inuportal.global.logging.service.LoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/logs")
@Tag(name="Logging", description = "로깅 관련 API")
public class LoggingController {

    private final LoggingService loggingService;

    @Operation(summary = "특정 날짜 접속 회원 Id 수와 목록 조회", description = "[관리자 전용] 특정 날짜에 접속한 회원 Id의 수와 목록을 조회합니다.")
    @GetMapping("/members")
    public ResponseEntity<ResponseDto<LoggingMemberResponse>> getMemberLogsByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date ) {
        return ResponseEntity.ok(ResponseDto.of(loggingService.getMemberLogsByDate(date), date + ": 회원 로그 조회 성공"));
    }

    @Operation(summary = "가장 많이 호출된 API 순위", description = "[관리자 전용] 특정 날짜에 호출된 API의 순위를 조회합니다. (상위 20개)")
    @GetMapping("/apis")
    public ResponseEntity<ResponseDto<List<LoggingApiResponse>>> getLogsByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date ) {
        return ResponseEntity.ok(ResponseDto.of(loggingService.getAPILogsByDate(date), date + ": api 로그 조회 성공"));
    }

    @Operation(summary = "API 로그 저장", description = "직접 입력한 API 로그를 저장합니다. <br><br>" +
            "Api uri를 보내주세요. ex) /api/buses, /api/maps")
    @PostMapping("/apis")
    public ResponseEntity<ResponseDto<String>> saveApiLogs(@Valid @RequestBody ApiLoggingRequest apiLoggingRequest,
                                                           @AuthenticationPrincipal Member member,
                                                           HttpServletRequest request) {
        return ResponseEntity.ok(ResponseDto.of(loggingService.saveApiLogs(apiLoggingRequest, member, request), "Api 로그 저장 성공"));
    }
}