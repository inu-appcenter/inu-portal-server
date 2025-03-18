package kr.inuappcenterportal.inuportal.domain.club.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.club.dto.ClubListResponseDto;
import kr.inuappcenterportal.inuportal.domain.club.dto.ClubRecruitingRequestDto;
import kr.inuappcenterportal.inuportal.domain.club.dto.ClubRecruitingResponseDto;
import kr.inuappcenterportal.inuportal.domain.club.service.ClubService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.petition.dto.PetitionRequestDto;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "Clubs", description = "동아리 API")
@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {
    private final ClubService clubService;

    @Operation(summary = "동아리 리스트 가져오기",description = "파라미터에 원하는 카테고리(교양학술, 문화, 봉사, 종교, 체육, 취미·전시) 혹은 빈칸(전체 조회)를 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "동아리 리스트 가져오기 성공",content = @Content(schema = @Schema(implementation = ListResponseDto.class)))
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<List<ClubListResponseDto>>> getAllClubs(@RequestParam(required = false) String category){
        return ResponseEntity.ok(ResponseDto.of(clubService.getClubList(category),"동아리 리스트 가져오기 성공"));
    }

    @Operation(summary = "동아리 모집공고 등록",description = "url 파라미터에 동아리의 id를, 바디에 모집 공고, 이미지, 모집 중 여부를 이미지와 함께 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "동아리 모집공고 등록 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping(value="/{clubId}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Long>>  addRecruit(@PathVariable Long clubId, @RequestPart(value = "images", required = false) List<MultipartFile> images, @Valid @RequestPart(value = "clubRecruitingRequestDto")ClubRecruitingRequestDto requestDto) throws IOException {
        return ResponseEntity.ok(ResponseDto.of(clubService.addRecruit(clubId,requestDto,images),"동아리 모집공고 등록 성공"));
    }

    @Operation(summary = "동아리 모집공고 수정",description = "url 파라미터에 동아리의 id를, 바디에 모집 공고, 이미지, 모집 중 여부를 이미지와 함께 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "동아리 모집공고 수정 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping(value="/{clubId}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Long>>  updateRecruit(@PathVariable Long clubId, @RequestPart(value = "images", required = false) List<MultipartFile> images, @Valid @RequestPart(value = "clubRecruitingRequestDto")ClubRecruitingRequestDto requestDto) throws IOException {
        return ResponseEntity.ok(ResponseDto.of(clubService.updateRecruit(clubId,requestDto,images),"동아리 모집공고 수정 성공"));
    }
    @Operation(summary = "동아리 모집공고 가져오기",description = "url 파라미터에 동아리의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "동아리 모집공고 가져오기 성공",content = @Content(schema = @Schema(implementation = ClubRecruitingResponseDto.class)))
    })
    @GetMapping("/{clubId}")
    public ResponseEntity<ResponseDto<ClubRecruitingResponseDto>> getClubRecruit(@PathVariable Long clubId){
        return ResponseEntity.ok(ResponseDto.of(clubService.getRecruit(clubId),"동아리 모집공고 가져오기 성공"));
    }
}
