package kr.inuappcenterportal.inuportal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Tag(name="Images", description = "횃불이 이미지 API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/images")
public class ImageController {
    private final RedisService redisService;
    @Operation(summary = "횃불이 이미지 등록")
    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "이미지 등록 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Long>> saveOnlyImage(@RequestPart List<MultipartFile> images) throws IOException {
        log.info("횃불이 이미지 저장 호출");
        redisService.saveFireImage(images);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.of(1L,"이미지 등록 성공"));
    }

    @Operation(summary = "횃불이 이미지 가져오기",description = "url 변수에 가져올 횃불이의 번호를 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "횃불이 이미지 가져오기 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 이미지 번호입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getFireImage(@PathVariable Long id){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.IMAGE_PNG);
        return ResponseEntity.ok().headers(httpHeaders).body(redisService.getFireImage(id));
    }

    @Operation(summary = "횃불이 이미지 삭제",description = "url 변수에 삭제할 횃불이의 번호를 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "횃불이 삭제 가져오기 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 이미지 번호입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto<Long>> deleteFireImage(@PathVariable Long id){
        log.info("횃불이 삭제 가져오기 호출 id:{}",id);
        redisService.deleteFireImage(id);
        return ResponseEntity.ok(ResponseDto.of(1L,"횃불이 이미지 삭제 성공"));
    }


}
