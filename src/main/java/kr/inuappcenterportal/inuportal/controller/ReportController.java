package kr.inuappcenterportal.inuportal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.dto.ReportListResponseDto;
import kr.inuappcenterportal.inuportal.dto.ReportRequestDto;
import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Reports", description = "신고 API")
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    @Operation(summary = "신고하기",description = "url 변수에 게시물의 id, 바디에 신고사유(reason), 신고 코멘트(comment)를 보내주세요. 결과 값은 신고 번호입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "신고하기 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/{postId}")
    public ResponseEntity<ResponseDto<Long>> reportPost(@Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH) @PathVariable Long postId
            , @AuthenticationPrincipal Member member, @Valid@RequestBody ReportRequestDto reportRequestDto){
        return ResponseEntity.ok(ResponseDto.of(reportService.saveReport(reportRequestDto,postId,member.getId()),"신고하기 성공"));
    }
    @Operation(summary = "신고목록 가져오기")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "신고목록 가져오기 성공",content = @Content(schema = @Schema(implementation = ReportListResponseDto.class)))
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<ReportListResponseDto>> getReportList(@RequestParam(required = false,defaultValue = "1") @Min(1) int page){
        return ResponseEntity.ok().body(ResponseDto.of(reportService.getReportList(page),"신고목록 가져오기 성공"));
    }

}
