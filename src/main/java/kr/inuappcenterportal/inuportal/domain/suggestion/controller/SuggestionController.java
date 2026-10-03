package kr.inuappcenterportal.inuportal.domain.suggestion.controller;

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
@RequestMapping("/api/suggestions")
public class SuggestionController implements SuggestionApiSpecification {
    private final SuggestionService suggestionService;

    /**
     * 건의사항 등록 컨트롤러
     */
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Long>> saveSuggestion(@AuthenticationPrincipal Member member,
                                                            @Valid @RequestPart SuggestionRequest suggestionRequest,
                                                            @RequestPart(required = false) List<MultipartFile> images) throws IOException {
        return ResponseEntity.ok(ResponseDto.of(suggestionService.saveSuggestion(suggestionRequest, member, images), "건의사항 등록 성공"));
    }

    /**
     * 건의사항 전체 조회 컨트롤러
     */
    @GetMapping("")
    public ResponseEntity<ResponseDto<SuggestionListResponse>> getSuggestionList(@AuthenticationPrincipal Member member, @RequestParam(required = false, defaultValue = "1") @Min(1) int page) {
        return ResponseEntity.ok(ResponseDto.of(suggestionService.getSuggestionList(page, member), "건의사항 목록 가져오기 성공"));
    }

    /**
     * 건의사항 단건 조회 컨트롤러
     */
    @GetMapping("/{suggestionId}")
    public ResponseEntity<ResponseDto<SuggestionResponse>> getSuggestion(@PathVariable Long suggestionId, @AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(ResponseDto.of(suggestionService.getSuggestion(suggestionId, member), "건의사항 상세 가져오기 성공"));
    }

    /**
     * 건의사항 내 이미지 조회 컨트롤러
     */
    @GetMapping("/{suggestionId}/images/{imageId}")
    public ResponseEntity<byte[]> getSuggestionImage(@PathVariable Long suggestionId,
                                                     @PathVariable Long imageId,
                                                     @AuthenticationPrincipal Member member) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(suggestionService.getSuggestionImageContentType(suggestionId, imageId, member));
        return ResponseEntity.ok().headers(httpHeaders).body(suggestionService.getSuggestionImage(suggestionId, imageId, member));
    }

    /**
     * 건의사항 삭제 컨트롤러
     */
    @DeleteMapping("/{suggestionId}")
    public ResponseEntity<ResponseDto<Long>> deleteSuggestion(@PathVariable Long suggestionId, @AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(ResponseDto.of(suggestionService.deleteSuggestion(suggestionId, member), "건의사항 삭제 성공"));
    }


    /**
     * 건의사항 상태 수정 컨트롤러
     */
    @PatchMapping("/{suggestionId}/status")
    public ResponseEntity<ResponseDto<Long>> changeSuggestionStatus(@PathVariable Long suggestionId, @Valid @RequestBody SuggestionStatusRequest suggestionStatusRequest) {
        return ResponseEntity.ok(ResponseDto.of(suggestionService.changeSuggestionStatus(suggestionId, suggestionStatusRequest), "건의사항 처리 상태 변경 성공"));
    }
}
