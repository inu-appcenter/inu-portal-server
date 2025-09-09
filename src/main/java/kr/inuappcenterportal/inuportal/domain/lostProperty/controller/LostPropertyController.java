package kr.inuappcenterportal.inuportal.domain.lostProperty.controller;

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
import kr.inuappcenterportal.inuportal.domain.lostProperty.dto.LostPropertyDetail;
import kr.inuappcenterportal.inuportal.domain.lostProperty.dto.LostPropertyPreview;
import kr.inuappcenterportal.inuportal.domain.lostProperty.dto.LostPropertyRegister;
import kr.inuappcenterportal.inuportal.domain.lostProperty.dto.LostPropertyUpdate;
import kr.inuappcenterportal.inuportal.domain.lostProperty.service.LostPropertyService;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@Tag(name = "LostProperties", description = "분실물 API")
@Slf4j
@RestController
@RequestMapping("/api/lost")
@RequiredArgsConstructor
public class LostPropertyController {

    private final LostPropertyService lostPropertyService;

    @Operation(summary = "분실물 저장", description = "헤더 Auth에 발급받은 토큰을, 바디에 {name,content} 보내주세요. 이미지를 보내주세요. 성공 시 등록된 분실물의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "분실물 등록 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ResponseDto<Long>> register(@RequestPart @Valid LostPropertyRegister request, @RequestPart(required = false) List<MultipartFile> images) throws IOException {
        if(ObjectUtils.isEmpty(images)) images = new ArrayList<>();
        return ResponseEntity.status(CREATED).body(ResponseDto.of(lostPropertyService.register(request, images), "분실물 등록 성공"));
    }

    @Operation(summary = "분실물 리스트 조회", description = "헤더 Auth에 발급받은 토큰을 보내주세요. 페이지(공백일 시 1)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "분실물 리스트 조회 성공", content = @Content(schema = @Schema(implementation = ListResponseDto.class)))
    })
    @GetMapping
    public ResponseEntity<ResponseDto<ListResponseDto<LostPropertyPreview>>> getList(@RequestParam(required = false, defaultValue = "1") @Min(1) int page) {
        return ResponseEntity.status(OK).body(ResponseDto.of(lostPropertyService.getList(page),"분실물 리스트 조회 성공"));
    }

    @Operation(summary = "분실물 상세 조회", description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 분실물의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "분실물 상세 조회 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/{lostId}")
    public ResponseEntity<ResponseDto<LostPropertyDetail>> get(@Parameter(name = "lostId",description = "분실물의 id",in = ParameterIn.PATH) @PathVariable Long lostId) {
        return ResponseEntity.status(OK).body(ResponseDto.of(lostPropertyService.get(lostId), "분실물 상세 조회 성공"));
    }

    @Operation(summary = "분실물 삭제", description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 분실물의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "분실물 삭제 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("/{lostId}")
    public ResponseEntity<ResponseDto<Long>> delete(@Parameter(name = "lostId",description = "분실물의 id",in = ParameterIn.PATH) @PathVariable Long lostId) {
        lostPropertyService.delete(lostId);
        return ResponseEntity.status(OK).body(ResponseDto.of(lostId, "분실물 삭제 완료"));
    }

    @Operation(summary = "분실물 수정", description = "헤더 Auth에 발급받은 토큰을, 바디에 {name,content }, 이미지를 보내주세요. url 파라미터에 분실물의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "분실물 수정 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping(value = "/{lostId}", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ResponseDto<Long>> update(@RequestPart @Valid LostPropertyUpdate request, @RequestPart(required = false) List<MultipartFile> images,
                                                    @Parameter(name = "lostId",description = "분실물의 id",in = ParameterIn.PATH) @PathVariable Long lostId) throws IOException {
        if(ObjectUtils.isEmpty(images)) images = new ArrayList<>();
        lostPropertyService.update(request, images, lostId);
        return ResponseEntity.ok(ResponseDto.of(lostId, "분실물 수정 완료"));
    }
}
