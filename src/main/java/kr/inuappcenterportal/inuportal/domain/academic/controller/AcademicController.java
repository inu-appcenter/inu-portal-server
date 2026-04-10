package kr.inuappcenterportal.inuportal.domain.academic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.academic.dto.AcademicBasicInfoRequestDto;
import kr.inuappcenterportal.inuportal.domain.academic.dto.AcademicBasicInfoResponseDto;
import kr.inuappcenterportal.inuportal.domain.academic.service.AcademicService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portal")
@Tag(name = "Portal", description = "포털 정보 API")
public class AcademicController {

    private final AcademicService academicService;

    @Operation(summary = "기본학적 정보 조회", description = "포털 아이디/비밀번호로 포털 시스템에 접근하여 기본학적 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "기본학적 정보 조회 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "포털 로그인 실패", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "학적 정보 없음", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "502", description = "ERP 세션 초기화 또는 조회 실패", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/basic-info")
    public ResponseEntity<ResponseDto<AcademicBasicInfoResponseDto>> getAcademicBasicInfo(
            @Valid @RequestBody AcademicBasicInfoRequestDto requestDto,
            @AuthenticationPrincipal Member member
    ) {
        log.info("Portal basic info request memberId={}", member.getId());
        AcademicBasicInfoResponseDto responseDto = academicService.getBasicInfo(requestDto);
        return ResponseEntity.ok(ResponseDto.of(responseDto, "기본 학적 정보 가져오기 성공"));
    }
}
