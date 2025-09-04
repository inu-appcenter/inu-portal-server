package kr.inuappcenterportal.inuportal.domain.keyword.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.keyword.dto.res.KeywordResponse;
import kr.inuappcenterportal.inuportal.domain.keyword.service.KeywordService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/keywords")
@Tag(name = "Keyword", description = "Keyword 관련 API")
public class KeywordController {

    private final KeywordService keywordService;

    // 키워드 알림 등록
    @Operation(summary = "키워드 알림 등록",
            description = "키워드와 카테고리를 입력하여 키워드 알림을 등록합니다. <br><br>" +
                    "keyword: 알림을 받고자 하는 키워드 <br>" + "department: 학과")
    @PostMapping
    public ResponseEntity<ResponseDto<KeywordResponse>> addKeyword(@AuthenticationPrincipal Member member,
                                                                   @RequestParam String keyword,
                                                                   @RequestParam Department department) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.of(keywordService.addKeyword(member, keyword, department), "키워드 알림 등록 성공"));
    }
}
