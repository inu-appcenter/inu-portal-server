package kr.inuappcenterportal.inuportal.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.dto.NoticeListResponseDto;
import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

    @Operation(summary = "모든 공지사항 가져오기")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "모든 공지사항 가져오기 성공",content = @Content(schema = @Schema(implementation = NoticeListResponseDto.class))),
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<List<NoticeListResponseDto>>> getAllNotice(){
        log.info("모든 공지사항 가져오기 호출");
        return new ResponseEntity<>(new ResponseDto<>(noticeService.getNoticeList(),"모든 공지사항 가져오기 성공"), HttpStatus.OK);
    }
}