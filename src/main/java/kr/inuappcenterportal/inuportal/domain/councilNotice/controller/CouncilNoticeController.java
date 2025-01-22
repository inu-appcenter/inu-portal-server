package kr.inuappcenterportal.inuportal.domain.councilNotice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import kr.inuappcenterportal.inuportal.domain.councilNotice.dto.CouncilNoticeListDto;
import kr.inuappcenterportal.inuportal.domain.councilNotice.dto.CouncilNoticeRequestDto;
import kr.inuappcenterportal.inuportal.domain.councilNotice.dto.CouncilNoticeResponseDto;
import kr.inuappcenterportal.inuportal.domain.councilNotice.service.CouncilNoticeService;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Tag(name = "CouncilNotices",description = "총학생회 공지사항 API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/councilNotices")
public class CouncilNoticeController {
    private final CouncilNoticeService councilNoticeService;

    @Operation(summary = "총학생회 공지사항 등록",description = "헤더 Auth에 발급받은 토큰을, 바디에 {title,content} 보내주세요. 그 이후 등록된 공지사항의 id와 이미지를 보내주세요. 성공 시 작성된 공지사항의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "총학생회 공지사항 등록 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping(value = "",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Long>> createCouncilNotice(@Valid@RequestPart(value = "councilNoticeRequestDto") CouncilNoticeRequestDto councilNoticeRequestDto, @RequestPart(required = false,value = "images") List<MultipartFile> images) throws IOException {
        log.info("총학생회 공지사항 등록");
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.of(councilNoticeService.saveCouncilNotice(councilNoticeRequestDto,images), "총학생회 공지사항 등록 성공"));
    }

    @Operation(summary = "총학생회 공지사항 수정",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 총학생회 공지사항의 id, 바디에 {title,content}을 형식으로 보내주세요. 성공 시 수정된 공지사항의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "총삭생회 공지사항 수정 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 총학생회 공지사항입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping(value = "/{councilNoticeId}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Long>> updateCouncilNotice(@Parameter(name = "councilNoticeId",description = "총학생회 공지사항의 id",in = ParameterIn.PATH) @PathVariable Long councilNoticeId, @Valid@RequestPart(value = "councilNoticeRequestDto") CouncilNoticeRequestDto councilNoticeRequestDto, @RequestPart(required = false,value = "images") List<MultipartFile> images) throws IOException {
        log.info("총학생회 공지사항 수정 호출 id:{}",councilNoticeId);
        councilNoticeService.updateCouncilNotice(councilNoticeId,councilNoticeRequestDto,images);
        return ResponseEntity.ok(ResponseDto.of(councilNoticeId,"총삭생회 공지사항 수정 성공"));
    }

    @Operation(summary = "총학생회 공지사항 삭제",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 게시글의 id를 보내주세요. 성공 시 삭제된 공지사항의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "총학생회 공지사항 삭제 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 총학생회 공지사항입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("/{councilNoticeId}")
    public ResponseEntity<ResponseDto<Long>> deleteCouncilNotice(@Parameter(name = "councilNoticeId",description = "총학생회 공지사항의 id",in = ParameterIn.PATH) @PathVariable Long councilNoticeId) throws IOException {
        log.info("총학생회 공지사항 삭제 호출 id:{}",councilNoticeId);
        councilNoticeService.deleteCouncilNotice(councilNoticeId);
        return ResponseEntity.ok(ResponseDto.of(councilNoticeId,"총학생회 공지사항 삭제 성공"));
    }

    @Operation(summary = "총학생회 공지사항 가져오기",description = "url 파라미터에 총학생회 공지사항의 id를 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "총학생회 공지사항 가져오기 성공",content = @Content(schema = @Schema(implementation = CouncilNoticeResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 총학생회 공지사항입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/{councilNoticeId}")
    public ResponseEntity<ResponseDto<CouncilNoticeResponseDto>> getCouncilNotice(HttpServletRequest httpServletRequest, @Parameter(name = "councilNoticeId",description = "총학생회 공지사항의 id",in = ParameterIn.PATH) @PathVariable Long councilNoticeId){
        log.info("총학생회 공지사항 가져오기 호출 id:{}",councilNoticeId);
        return ResponseEntity.ok(ResponseDto.of(councilNoticeService.getCouncilNotice(councilNoticeId,httpServletRequest.getHeader("X-Forwarded-For")),"총학생회 공지사항 가져오기 성공"));
    }

    @Operation(summary = "총학생회 공지사항 리스트 가져오기",description = "정렬기준 sort(date/공백(최신순), view), 페이지(공백일 시 1)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "총학생회 공지사항 리스트 가져오기 성공",content = @Content(schema = @Schema(implementation = ListResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<ListResponseDto<CouncilNoticeListDto>>> getAllPost(@RequestParam(required = false,defaultValue = "date") String sort
            , @RequestParam(required = false,defaultValue = "1") @Min(1) int page ){
        return ResponseEntity.ok(ResponseDto.of(councilNoticeService.getCouncilNoticeList(sort,page),"총학생회 공지사항 리스트 가져오기 성공"));
    }


}
