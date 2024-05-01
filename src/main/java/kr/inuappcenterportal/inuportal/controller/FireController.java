package kr.inuappcenterportal.inuportal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.dto.FireDto;
import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.dto.UriDto;
import kr.inuappcenterportal.inuportal.service.FireService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name="Fires", description = "횃불이Ai API")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@RequestMapping("/api/fires")
public class FireController {
    private final FireService fireService;

    @Operation(summary = "횃불이 ai 그림 그리기",description = "바디에 {param}을 json 형식으로 보내주세요. 성공 시 만들어진 이미지의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "횃불이 ai 그림 그리기 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "400",description = "횃불이 ai 이미지 생성 uri에 문제가 있습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("")
    public ResponseEntity<ResponseDto<Long>> drawFireAiImage(@Valid@RequestBody FireDto fireDto){
        log.info("횃불이 그림 그리기 호출 파라미터 :{}",fireDto.getParam());
        return new ResponseEntity<>(new ResponseDto<>(fireService.drawImage(fireDto.getParam()),"횃불이 ai 그림 그리기 성공"), HttpStatus.CREATED);
    }

    @Operation(summary = "횃불이 ai 이미지 가져오기",description = "url 변수에 가져올 이미지 번호를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "횃불이 ai 이미지 가져오기 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404",description = "존재하지 않는 이미지 번호입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> drawFireAiImage(@PathVariable Long id){
        log.info("횃불이 ai 이미지 가져오기 호출 id : {}",id);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.IMAGE_PNG);
        return new ResponseEntity<>(fireService.getFireAiImage(id),httpHeaders,HttpStatus.OK);
    }

    @Operation(summary = "ai 생성 요청 uri 변경",description = "바디에 {uri}을 json 형식으로 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "ai 생성 요청 uri 변경 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/uri")
    public ResponseEntity<ResponseDto<Long>> changeRequestUri(@Valid @RequestBody UriDto uriDto){
        log.info("횃불이 ai 이미지 요청 uri 변경 호출 uri :{}",uriDto.getUri());
        fireService.changeUri(uriDto.getUri());
        return new ResponseEntity<>(new ResponseDto<>(1L,"ai 생성 요청 uri 변경 성공"),HttpStatus.OK);
    }






}
