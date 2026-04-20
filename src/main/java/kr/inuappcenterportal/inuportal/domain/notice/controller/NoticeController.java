package kr.inuappcenterportal.inuportal.domain.notice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.inuappcenterportal.inuportal.domain.notice.dto.DepartmentNoticeListResponse;
import kr.inuappcenterportal.inuportal.domain.notice.dto.NoticeListResponseDto;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import kr.inuappcenterportal.inuportal.domain.notice.service.NoticeService;
import kr.inuappcenterportal.inuportal.domain.schedule.dto.ScheduleListResponseDoc;
import kr.inuappcenterportal.inuportal.domain.schedule.dto.ScheduleResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedule.service.ScheduleService;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@Tag(name = "Notices", description = "학교 공지사항 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class NoticeController {
    private final NoticeService noticeService;
    private final ScheduleService scheduleService;

    @Operation(summary = "모든 공지사항 가져오기", description = "url 파라미터로 카테고리, 정렬기준, 페이지를 보냅니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "모든 공지사항 가져오기 성공",
                    content = @Content(schema = @Schema(implementation = NoticeListResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "정렬 기준값이 올바르지 않습니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            )
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<ListResponseDto<NoticeListResponseDto>>> getAllNotice(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "date") String sort,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page
    ) {
        return ResponseEntity.ok(ResponseDto.of(noticeService.getNoticeList(category, sort, page), "모든 공지사항 가져오기 성공"));
    }

    @Operation(summary = "학교 공지사항 검색", description = "query(검색어), category(카테고리 - 선택), page(페이지 - 선택)를 파라미터로 받습니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "공지사항 검색 성공",
                    content = @Content(schema = @Schema(implementation = NoticeListResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "검색어가 올바르지 않습니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            )
    })
    @GetMapping("/search")
    public ResponseEntity<ResponseDto<ListResponseDto<NoticeListResponseDto>>> getAllNoticeBySearch(
            @RequestParam @NotBlank(message = "검색어는 필수입니다.") @Size(min = 2, message = "2글자 이상 입력해주세요.") String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page
    ) {
        return ResponseEntity.ok(ResponseDto.of(noticeService.searchNotice(query, category, page), "공지사항 검색 성공"));
    }

    @Operation(summary = "상단부 최신 공지 12개 가져오기")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "최신 공지 가져오기 성공",
                content = @Content(schema = @Schema(implementation = NoticeListResponseDto.class))
        )
    })
    @GetMapping("/top")
    public ResponseEntity<ResponseDto<List<NoticeListResponseDto>>> getPostForTop() {
        return ResponseEntity.ok(ResponseDto.of(noticeService.getTop(), "최신 공지 가져오기 성공"));
    }

    @Operation(summary = "학과별 공지사항 가져오기", description = "url 파라미터로 학과, 정렬기준, 페이지를 보냅니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "학과별 공지사항 가져오기 성공",
                    content = @Content(schema = @Schema(implementation = DepartmentNoticeListResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "정렬 기준값이 올바르지 않습니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            )
    })
    @GetMapping("/department")
    public ResponseEntity<ResponseDto<ListResponseDto<DepartmentNoticeListResponse>>> getDepartmentNotices(
            @RequestParam Department department,
            @RequestParam(required = false, defaultValue = "date") String sort,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page
    ) {
        return ResponseEntity.ok(
                ResponseDto.of(
                        noticeService.getDepartmentNotices(department, sort, page),
                        "해당 학과 모든 공지사항 가져오기 성공"
                )
        );
    }

    @Operation(summary = "학과 공지 연결 일정 조회", description = "학과 공지 id로 연결된 일정 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "학과 공지 연결 일정 조회 성공",
                    content = @Content(schema = @Schema(implementation = ScheduleListResponseDoc.class))
            )
    })
    @GetMapping("/department/{departmentNoticeId}/schedules")
    public ResponseEntity<ResponseDto<List<ScheduleResponseDto>>> getSchedulesByDepartmentNoticeId(
            @PathVariable Long departmentNoticeId
    ) {
        return ResponseEntity.ok(
                ResponseDto.of(
                        scheduleService.getSchedulesByDepartmentNoticeId(departmentNoticeId),
                        "학과 공지 연결 일정 조회 성공"
                )
        );
    }
}
