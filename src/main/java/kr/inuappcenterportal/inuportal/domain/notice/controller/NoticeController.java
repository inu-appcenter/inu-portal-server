package kr.inuappcenterportal.inuportal.domain.notice.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import kr.inuappcenterportal.inuportal.domain.notice.dto.DepartmentNoticeListResponse;
import kr.inuappcenterportal.inuportal.domain.notice.dto.NoticeListResponseDto;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import kr.inuappcenterportal.inuportal.domain.notice.service.NoticeService;
import kr.inuappcenterportal.inuportal.domain.post.dto.PostListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name="Notices", description = "학교 공지사항 API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/notices")
public class NoticeController {
    private final NoticeService noticeService;

    @Operation(summary = "모든 공지사항 가져오기",description = "url 파라미터에 카테고리(빈 값일 시 모든 공지사항), 정렬기준(sort) 을 date/공백(최신순), view 둘 중 하나),페이지(공백일 시 1)를 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "모든 공지사항 가져오기 성공",content = @Content(schema = @Schema(implementation = NoticeListResponseDto.class))),
            @ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))

    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<ListResponseDto<NoticeListResponseDto>>> getAllNotice(@RequestParam(required = false) String category, @RequestParam(required = false,defaultValue = "date") String sort
    , @RequestParam(required = false,defaultValue = "1") @Min(1) int page){
        return ResponseEntity.ok(ResponseDto.of(noticeService.getNoticeList(category, sort,page),"모든 공지사항 가져오기 성공"));
    }

    @Operation(summary = "상단부 인기 공지 12개 가져오기")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "인기 공지 가져오기 성공",content = @Content(schema = @Schema(implementation = NoticeListResponseDto.class)))
    })
    @GetMapping("/top")
    public ResponseEntity<ResponseDto<List<NoticeListResponseDto>>> getPostForTop( ){
        return ResponseEntity.ok(ResponseDto.of(noticeService.getTop(),"인기 공지 가져오기 성공"));
    }

    @Operation(summary = "학과 별 공지사항 가져오기",description = "url 파라미터에 학과(빈 값 불가), 정렬기준(sort) 을 date/공백(최신순), view 둘 중 하나), 페이지(공백일 시 1)를 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "학과 별 모든 공지사항 가져오기 성공",content = @Content(schema = @Schema(implementation = DepartmentNoticeListResponse.class))),
            @ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))

    })
    @GetMapping("/department")
    public ResponseEntity<ResponseDto<ListResponseDto<DepartmentNoticeListResponse>>> getDepartmentNotices(
            @RequestParam Department department,
            @RequestParam(required = false,defaultValue = "date") String sort,
            @RequestParam(required = false,defaultValue = "1") @Min(1) int page) {
        return ResponseEntity.ok(ResponseDto.of(noticeService.getDepartmentNotices(department, sort, page),"해당 학과 모든 공지사항 가져오기 성공"));
    }

    @GetMapping("/test-crawling")
    public String testDepartmentNoticeUrl() {
        noticeService.crawlingDepartmentNotices();
        return "성공";
    }
}
