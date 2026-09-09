package kr.inuappcenterportal.inuportal.domain.suggestion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionListResponse;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionRequest;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionResponse;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionStatusRequest;
import kr.inuappcenterportal.inuportal.domain.suggestion.service.SuggestionService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Suggestions", description = "건의사항 API")
@RequestMapping("/api/suggestions")
public class SuggestionController {
    private final SuggestionService suggestionService;

    @Operation(summary = "건의사항 등록", description = """
            **Content-Type: `multipart/form-data`** — JSON 바디가 아닙니다.

            두 개의 파트로 전송합니다.
            - `suggestionRequest` (필수): SuggestionRequest 스키마의 JSON. 이 파트의 Content-Type을 `application/json`으로 지정해야 합니다.
            - `images` (선택): 이미지 파일. 0~5장. `image/*` 타입만 허용. 파일당 최대 10MB, 요청 전체 최대 20MB. 첨부하지 않으면 파트 자체를 생략합니다.

            성공 시 응답 `data`에 등록된 건의사항 id가 담깁니다. 예: `{ "data": 42, "msg": "건의사항 등록 성공" }`
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 성공. `data` = 등록된 건의사항 id", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "400", description = """
                    요청 값 오류. `msg`로 원인을 구분합니다.
                    - `잘못된 형식의 문의 유형을 요청했습니다.` — category 값이 허용 목록 밖
                    - `이미지 파일만 업로드할 수 있습니다.` — images 파트에 이미지가 아닌 파일 포함
                    - `이미지는 최대 5장까지 첨부할 수 있습니다.` — 이미지 6장 이상
                    - 그 외 content 미입력 / 2000자 초과 등 필드 검증 메시지
                    """, content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schemaProperties = {
                    @SchemaProperty(name = "suggestionRequest", schema = @Schema(implementation = SuggestionRequest.class)),
                    @SchemaProperty(name = "images", array = @ArraySchema(schema = @Schema(type = "string", format = "binary")))
            },
            encoding = @Encoding(name = "suggestionRequest", contentType = MediaType.APPLICATION_JSON_VALUE)
    ))
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Long>> saveSuggestion(@AuthenticationPrincipal Member member,
                                                            @Valid @RequestPart SuggestionRequest suggestionRequest,
                                                            @RequestPart(required = false) List<MultipartFile> images) throws IOException {
        return ResponseEntity.ok(ResponseDto.of(suggestionService.saveSuggestion(suggestionRequest, member, images), "건의사항 등록 성공"));
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

    @Operation(summary = "건의사항 이미지 가져오기", description = """
            이미지 **바이너리**를 그대로 반환합니다 (`Content-Type: image/webp`). 다른 API와 달리 `{ data, msg }` 형태가 아닙니다.

            - `imageId`는 **1부터 시작**하며, 상세 조회(`GET /api/suggestions/{id}`) 응답의 `imageCount`까지 유효합니다. (imageCount=3이면 1·2·3)
            - 작성자 본인 또는 관리자만 조회할 수 있습니다.
            - 인증 토큰이 필요하므로 `<img src>`에 URL만 넣으면 401이 납니다. 인증 헤더를 실어 fetch한 뒤 blob URL로 표시하세요.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이미지 바이너리",
                    content = @Content(mediaType = "image/webp", schema = @Schema(type = "string", format = "binary")))
            , @ApiResponse(responseCode = "403", description = "`이 건의사항에 대한 권한이 없습니다.` (작성자·관리자 아님)", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "404", description = "`존재하지 않는 건의사항입니다.` 또는 `존재하지 않는 이미지 번호입니다.` (imageId 범위 밖)", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/{suggestionId}/images/{imageId}")
    public ResponseEntity<byte[]> getSuggestionImage(@Parameter(name = "suggestionId", description = "건의사항의 id", in = ParameterIn.PATH) @PathVariable Long suggestionId,
                                                     @Parameter(name = "imageId", description = "이미지 순번 (1부터 imageCount까지)", in = ParameterIn.PATH) @PathVariable Long imageId,
                                                     @AuthenticationPrincipal Member member) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.parseMediaType("image/webp"));
        return ResponseEntity.ok().headers(httpHeaders).body(suggestionService.getSuggestionImage(suggestionId, imageId, member));
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

    @Operation(summary = "건의사항 처리 상태 변경 (관리자 전용)", description = "url 파라미터에 건의사항의 id, 바디에 처리 상태(status)를 보내주세요. 처리 상태는 개발/운영팀의 내부 진행 상황이며 상담 진행 상태를 의미하지 않습니다. 관리자 권한이 있는 사용자만 호출할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "건의사항 처리 상태 변경 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "400", description = "잘못된 형식의 건의사항 상태를 요청했습니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "404", description = "존재하지 않는 건의사항입니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PatchMapping("/{suggestionId}/status")
    public ResponseEntity<ResponseDto<Long>> changeSuggestionStatus(@Parameter(name = "suggestionId", description = "건의사항의 id", in = ParameterIn.PATH) @PathVariable Long suggestionId, @Valid @RequestBody SuggestionStatusRequest suggestionStatusRequest) {
        return ResponseEntity.ok(ResponseDto.of(suggestionService.changeSuggestionStatus(suggestionId, suggestionStatusRequest), "건의사항 처리 상태 변경 성공"));
    }
}
