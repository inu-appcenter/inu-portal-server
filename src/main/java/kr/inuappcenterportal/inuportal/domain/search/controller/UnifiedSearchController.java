package kr.inuappcenterportal.inuportal.domain.search.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.inuappcenterportal.inuportal.domain.search.dto.SearchTab;
import kr.inuappcenterportal.inuportal.domain.search.dto.UnifiedSearchResponseDto;
import kr.inuappcenterportal.inuportal.domain.search.service.UnifiedSearchService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@Tag(name = "UnifiedSearch", description = "Elasticsearch 기반 통합 검색 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class UnifiedSearchController {

    private final UnifiedSearchService unifiedSearchService;

    @Operation(
            summary = "통합 검색 (Google형 전 도메인 검색)",
            description = "Nori 형태소 분석 및 캠퍼스 특화 동의어 사전을 적용한 통합 검색을 수행합니다. " +
                    "tab이 ALL(기본값)인 경우 학교공지, 학과공지, 게시글, 학사일정, 전화번호부, 강의, 동아리 섹션별 상위 항목을 묶어서 반환합니다. " +
                    "특정 탭 선택 시 해당 도메인의 페이징 결과를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공",
                    content = @Content(schema = @Schema(implementation = UnifiedSearchResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "검색어가 올바르지 않습니다.")
    })
    @GetMapping("/unified")
    public ResponseEntity<ResponseDto<UnifiedSearchResponseDto>> search(
            @Parameter(description = "검색어 (2글자 이상)", example = "장학금")
            @RequestParam @NotBlank(message = "검색어는 필수입니다.") @Size(min = 2, message = "2글자 이상 입력해주세요.") String q,
            @Parameter(description = "검색 탭 (ALL, NOTICE, DEPT_NOTICE, POST, SCHEDULE, DIRECTORY, COURSE, CLUB)", example = "ALL")
            @RequestParam(required = false, defaultValue = "ALL") SearchTab tab,
            @Parameter(description = "페이지 번호 (특정 탭 선택 시 유효, 1부터 시작)", example = "1")
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @Parameter(description = "페이지당 결과 수 (ALL일 때는 섹션당 미리보기 수)", example = "10")
            @RequestParam(required = false, defaultValue = "10") @Min(1) int size
    ) {
        UnifiedSearchResponseDto response = unifiedSearchService.search(q, tab, page, size);
        return ResponseEntity.ok(ResponseDto.of(response, "통합 검색 성공"));
    }
}
