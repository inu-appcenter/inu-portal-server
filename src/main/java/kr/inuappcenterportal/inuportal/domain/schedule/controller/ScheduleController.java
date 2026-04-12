package kr.inuappcenterportal.inuportal.domain.schedule.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.notice.dto.NoticeListResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedule.dto.ScheduleResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedule.service.ScheduleService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(name = "Schedules", description = "학사일정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class ScheduleController {
    private final ScheduleService scheduleService;

    @Operation(summary = "학사일정 가져오기", description = "url 파라미터로 year, month를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "학사일정 가져오기 성공", content = @Content(schema = @Schema(implementation = NoticeListResponseDto.class)))
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<List<ScheduleResponseDto>>> getScheduleByMonth(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(
                ResponseDto.of(scheduleService.getScheduleByMonth(year, month), "학사일정 가져오기 성공")
        );
    }

    @Operation(summary = "내 학과 일정 가져오기", description = "로그인한 사용자의 학과를 기준으로 해당 월의 학과 일정을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 학과 일정 가져오기 성공", content = @Content(schema = @Schema(implementation = NoticeListResponseDto.class)))
    })
    @GetMapping("/my-department")
    public ResponseEntity<ResponseDto<List<ScheduleResponseDto>>> getMyDepartmentScheduleByMonth(
            @AuthenticationPrincipal Member member,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(
                ResponseDto.of(
                        scheduleService.getMyDepartmentScheduleByMonth(member, year, month),
                        "내 학과 일정 가져오기 성공"
                )
        );
    }
}
