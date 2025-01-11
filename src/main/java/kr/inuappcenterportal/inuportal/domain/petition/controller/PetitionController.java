package kr.inuappcenterportal.inuportal.domain.petition.controller;

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
import kr.inuappcenterportal.inuportal.domain.councilNotice.dto.CouncilNoticeResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.petition.dto.PetitionListResponseDto;
import kr.inuappcenterportal.inuportal.domain.petition.dto.PetitionRequestDto;
import kr.inuappcenterportal.inuportal.domain.petition.dto.PetitionResponseDto;
import kr.inuappcenterportal.inuportal.domain.petition.service.PetitionService;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Tag(name = "Petitions",description = "총학생회 청원 API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/petitions")
public class PetitionController {
    private final PetitionService petitionService;

    @Operation(summary = "총학생회 청원 등록",description = "헤더 Auth에 발급받은 토큰을, 바디에 {title,content,isPrivate}, 이미지 리스트를 보내주세요. 그 이후 등록된 청원의 id와 이미지를 보내주세요. 성공 시 청원의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "총학생회 청원 등록 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Long>> createCouncilNotice(@RequestPart(value = "images") List<MultipartFile> images, @Valid @RequestPart(value = "petitionRequestDto") PetitionRequestDto petitionRequestDto, @AuthenticationPrincipal Member member) throws IOException {
        log.info("총학생회 청원 등록");
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.of(petitionService.savePetition(petitionRequestDto, member, images), "총학생회 청원 등록 성공"));
    }

    @Operation(summary = "총학생회 청원 수정",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 총학생회 청원의 id, 바디에 {title,content,isPrivate}, 이미지 리스트를 보내주세요. 성공 시 수정된 청원의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "총삭생회 청원 수정 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "이 게시글의 수정/삭제에 대한 권한이 없습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 총학생회 청원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping(value ="/{petitionId}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Long>> updateCouncilNotice(@RequestPart List<MultipartFile> images,@Parameter(name = "petitionId",description = "총학생회 청원의 id",in = ParameterIn.PATH) @PathVariable Long petitionId, @Valid@RequestPart PetitionRequestDto petitionRequestDto, @AuthenticationPrincipal Member member) throws IOException {
        log.info("총학생회 청원 수정 호출 id:{}",petitionId);
        petitionService.updatePetition(petitionId,petitionRequestDto,member,images);
        return ResponseEntity.ok(ResponseDto.of(petitionId,"총삭생회 청원 수정 성공"));
    }

    @Operation(summary = "총학생회 청원 삭제",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 총학생회 청원의 id를 보내주세요. 성공 시 삭제된 청원의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "총학생회 청원 삭제 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "이 게시글의 수정/삭제에 대한 권한이 없습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 총학생회 청원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("/{petitionId}")
    public ResponseEntity<ResponseDto<Long>> deleteCouncilNotice(@Parameter(name = "petitionId",description = "총학생회 청원의 id",in = ParameterIn.PATH) @PathVariable Long petitionId, @AuthenticationPrincipal Member member){
        log.info("총학생회 청원 삭제 호출 id:{}",petitionId);
        petitionService.deletePetition(petitionId,member);
        return ResponseEntity.ok(ResponseDto.of(petitionId,"총학생회 청원 삭제 성공"));
    }

    @Operation(summary = "총학생회 청원 가져오기",description = "url 파라미터에 총학생회 청원의 id를 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "총학생회 청원 가져오기 성공",content = @Content(schema = @Schema(implementation = CouncilNoticeResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 총학생회 청원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/{petitionId}")
    public ResponseEntity<ResponseDto<PetitionResponseDto>> getCouncilNotice(HttpServletRequest httpServletRequest,@Parameter(name = "petitionId",description = "총학생회 청원의 id",in = ParameterIn.PATH) @PathVariable Long petitionId, @AuthenticationPrincipal Member member){
        log.info("총학생회 청원 가져오기 호출 id:{}", petitionId);
        return ResponseEntity.ok(ResponseDto.of(petitionService.getPetition(petitionId,httpServletRequest.getHeader("X-Forwarded-For"),member),"총학생회 청원 가져오기 성공"));
    }

    @Operation(summary = "총학생회 청원 이미지 가져오기",description = "url 파라미터에 petitionNoticeId, imageId를 보내주세요. imageId는 이미지의 등록 순번입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "404",description = "존재하지 않는 이미지 번호입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/{petitionId}/images/{imageId}")
    public ResponseEntity<byte[]> getCouncilNoticeImages(@Parameter(name = "petitionId",description = "총학생회 청원의 id",in = ParameterIn.PATH) @PathVariable Long petitionId, @Parameter(name = "imageId",description = "이미지 번호",in = ParameterIn.PATH) @PathVariable Long imageId){
        log.info("총학생회 청원 이미지 가져오기 호출 id:{}",petitionId);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.IMAGE_PNG);
        return ResponseEntity.ok().headers(httpHeaders).body(petitionService.getPetitionImage(petitionId,imageId));
    }

    @Operation(summary = "총학생회 청원 리스트 가져오기",description = "정렬기준 sort(date/공백(최신순), view), 페이지(공백일 시 1)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "총학생회 청원 리스트 가져오기 성공",content = @Content(schema = @Schema(implementation = ListResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<ListResponseDto<PetitionListResponseDto>>> getAllPost(@RequestParam(required = false,defaultValue = "date") String sort
            , @RequestParam(required = false,defaultValue = "1") @Min(1) int page, @AuthenticationPrincipal Member member ){
        return ResponseEntity.ok(ResponseDto.of(petitionService.getPetitionList(sort,page,member),"총학생회 청원 리스트 가져오기 성공"));
    }
}
