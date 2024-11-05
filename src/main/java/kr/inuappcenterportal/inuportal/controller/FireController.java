package kr.inuappcenterportal.inuportal.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.Fire;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.dto.*;
import kr.inuappcenterportal.inuportal.service.FireService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name="Fires", description = "횃불이Ai API")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@RequestMapping("/api/fires")
public class FireController {
    private final FireService fireService;

    @Operation(summary = "횃불이 ai 그림 그리기",description = "헤더에 인증토큰을, 바디에 {prompt}을 json 형식으로 보내주세요. 성공 시 만들어진 이미지의 생성정보가 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "횃불이 ai 그림 요청이 큐에 성공적으로 추가됨.",content = @Content(schema = @Schema(implementation = FireResponseDto.class))),
            @ApiResponse(responseCode = "400",description = "횃불이 이미지가 생성 중 입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/predict")
    public ResponseEntity<ResponseDto<FireResponseDto>> drawFireAiImage(@Valid@RequestBody FireDto fireDto, @AuthenticationPrincipal Member member) throws JsonProcessingException {
        log.info("횃불이 그림 그리기 호출 파라미터 :{}",fireDto.getPrompt());
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.of(fireService.drawImage(member.getId(),fireDto.getPrompt()),"횃불이 ai 그림 요청이 큐에 성공적으로 추가됨."));
    }


    @Operation(summary = "횃불이 ai 이미지 정보들 가져오기",description = "url 파라미터에 페이지 번호를 보내주세요. 보내지 않을 시 첫 페이지가 보내집니다. 한 페이지의 크기는 10입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "횃불이 ai 이미지 정보들 가져오기 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<Page<Fire>>> getFireRating(@AuthenticationPrincipal Member member,@RequestParam(required = false,defaultValue = "0") int page){
        return ResponseEntity.ok(ResponseDto.of(fireService.getFireImageList(page),"횃불이 ai 이미지 정보들 가져오기 성공"));
    }








}
