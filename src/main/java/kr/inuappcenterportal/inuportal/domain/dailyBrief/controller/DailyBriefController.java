package kr.inuappcenterportal.inuportal.domain.dailyBrief.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.dto.req.DailyBriefSettingRequestDto;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.dto.res.DailyBriefSettingResponseDto;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.service.DailyBriefService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Daily Brief", description = "Daily Brief (시간표 및 학사일정 알림) 설정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/daily-brief")
public class DailyBriefController {

    private final DailyBriefService dailyBriefService;

    @Operation(summary = "Daily Brief 설정 조회", description = "로그인한 사용자의 Daily Brief(시간표 및 학사일정 알림) 설정을 조회합니다. 설정이 없으면 기본값으로 생성됩니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "설정 조회 성공",
                    content = @Content(schema = @Schema(implementation = DailyBriefSettingResponseDto.class))
            )
    })
    @GetMapping("/settings")
    public ResponseEntity<ResponseDto<DailyBriefSettingResponseDto>> getSettings(
            @AuthenticationPrincipal Member member
    ) {
        DailyBriefSettingResponseDto response = dailyBriefService.getSettings(member);
        return ResponseEntity.ok(ResponseDto.of(response, "Daily Brief 설정 조회 성공"));
    }

    @Operation(summary = "Daily Brief 설정 수정", description = "로그인한 사용자의 Daily Brief 설정을 수정합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "설정 수정 성공",
                    content = @Content(schema = @Schema(implementation = DailyBriefSettingResponseDto.class))
            )
    })
    @PutMapping("/settings")
    public ResponseEntity<ResponseDto<DailyBriefSettingResponseDto>> updateSettings(
            @AuthenticationPrincipal Member member,
            @RequestBody DailyBriefSettingRequestDto requestDto
    ) {
        DailyBriefSettingResponseDto response = dailyBriefService.updateSettings(member, requestDto);
        return ResponseEntity.ok(ResponseDto.of(response, "Daily Brief 설정 수정 성공"));
    }
}
