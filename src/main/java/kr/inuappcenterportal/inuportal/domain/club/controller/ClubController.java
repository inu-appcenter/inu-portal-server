package kr.inuappcenterportal.inuportal.domain.club.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.club.dto.ClubListResponseDto;
import kr.inuappcenterportal.inuportal.domain.club.service.ClubService;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
