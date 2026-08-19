package kr.inuappcenterportal.inuportal.domain.suggestion.controller;

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
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionAnswerRequest;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionListResponse;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionRequest;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionResponse;
import kr.inuappcenterportal.inuportal.domain.suggestion.service.SuggestionService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Suggestions", description = "건의사항 API")
@RequestMapping("/api/suggestions")
public class SuggestionController {
    private final SuggestionService suggestionService;

    @Operation(summary = "건의사항 등록", description = "바디에 건의 내용(content), 응원 메시지(cheerMessage, 선택), 문의 유형(category), 앱/기기 정보(appVersion, osType, osVersion, deviceModel, 선택)를 보내주세요. 결과 값은 등록된 건의사항의 id입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "건의사항 등록 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "400", description = "잘못된 형식의 문의 유형을 요청했습니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("")
    public ResponseEntity<ResponseDto<Long>> saveSuggestion(@AuthenticationPrincipal Member member, @Valid @RequestBody SuggestionRequest suggestionRequest) {
        return ResponseEntity.ok(ResponseDto.of(suggestionService.saveSuggestion(suggestionRequest, member), "건의사항 등록 성공"));
    }

    @Operation(summary = "건의사항 목록 가져오기", description = "헤더 Auth에 발급받은 토큰을, 페이지(공백일 시 1)를 보내주세요. 일반 사용자는 본인이 작성한 건의사항만, 관리자는 전체 건의사항을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "건의사항 목록 가져오기 성공", content = @Content(schema = @Schema(implementation = SuggestionListResponse.class)))
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<SuggestionListResponse>> getSuggestionList(@AuthenticationPrincipal Member member, @RequestParam(required = false, defaultValue = "1") @Min(1) int page) {
        return ResponseEntity.ok(ResponseDto.of(suggestionService.getSuggestionList(page, member), "건의사항 목록 가져오기 성공"));
    }

    @Operation(summary = "건의사항 상세 가져오기", description = "url 파라미터에 건의사항의 id를 보내주세요. 본인이 작성했거나 관리자만 조회할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "건의사항 상세 가져오기 성공", content = @Content(schema = @Schema(implementation = SuggestionResponse.class)))
            , @ApiResponse(responseCode = "403", description = "이 건의사항에 대한 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "404", description = "존재하지 않는 건의사항입니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/{suggestionId}")
    public ResponseEntity<ResponseDto<SuggestionResponse>> getSuggestion(@Parameter(name = "suggestionId", description = "건의사항의 id", in = ParameterIn.PATH) @PathVariable Long suggestionId, @AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(ResponseDto.of(suggestionService.getSuggestion(suggestionId, member), "건의사항 상세 가져오기 성공"));
    }

    @Operation(summary = "건의사항 삭제", description = "url 파라미터에 건의사항의 id를 보내주세요. 본인이 작성했거나 관리자만 삭제할 수 있으며, 처리 상태와 무관하게 항상 삭제 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "건의사항 삭제 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "403", description = "이 건의사항에 대한 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "404", description = "존재하지 않는 건의사항입니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("/{suggestionId}")
    public ResponseEntity<ResponseDto<Long>> deleteSuggestion(@Parameter(name = "suggestionId", description = "건의사항의 id", in = ParameterIn.PATH) @PathVariable Long suggestionId, @AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(ResponseDto.of(suggestionService.deleteSuggestion(suggestionId, member), "건의사항 삭제 성공"));
    }

    @Operation(summary = "건의사항 답변/상태변경 (관리자 전용)", description = "url 파라미터에 건의사항의 id, 바디에 처리 상태(status), 운영진 답변 내용(answerContent, 선택)을 보내주세요. 관리자 권한이 있는 사용자만 호출할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "건의사항 답변/상태변경 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "400", description = "잘못된 형식의 건의사항 상태를 요청했습니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "404", description = "존재하지 않는 건의사항입니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PatchMapping("/{suggestionId}/answer")
    public ResponseEntity<ResponseDto<Long>> answerSuggestion(@Parameter(name = "suggestionId", description = "건의사항의 id", in = ParameterIn.PATH) @PathVariable Long suggestionId, @Valid @RequestBody SuggestionAnswerRequest suggestionAnswerRequest) {
        return ResponseEntity.ok(ResponseDto.of(suggestionService.answerSuggestion(suggestionId, suggestionAnswerRequest), "건의사항 답변/상태변경 성공"));
    }
}
