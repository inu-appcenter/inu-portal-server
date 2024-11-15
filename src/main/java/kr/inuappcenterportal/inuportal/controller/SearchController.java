package kr.inuappcenterportal.inuportal.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.service.FolderService;
import kr.inuappcenterportal.inuportal.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@Tag(name = "Search",description = "게시글 검색 API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/search")
public class SearchController {
    private final PostService postService;
    private final FolderService folderService;

    @Operation(summary = "게시글 검색",description = "url 파라미터에 검색내용 query , 정렬기준을 sort(date/공백(최신순),like,scrap)를, 페이지(공백일 시 1)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 검색 성공",content = @Content(schema = @Schema(implementation = ListResponseDto.class))),
            @ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다. / 검색옵션이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<ListResponseDto>> search(@RequestParam @NotBlank(message = "공백일 수 없습니다.") @Size(min = 2,message = "2글자 이상 입력해야 합니다.") String query, @RequestParam(required = false) String sort
    , @RequestParam(required = false,defaultValue = "1") @Min(1) int page){
        return ResponseEntity.ok(ResponseDto.of(postService.searchPost(query,sort,page),"게시글 검색 성공"));
    }

    @Operation(summary = "스크랩 게시글 검색",description = "url 헤더에 Auth 토큰, url 파라미터에 검색내용 query , 정렬기준을 sort(date/공백(최신순),like,scrap)를, 페이지(공백일 시 1)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "스크랩 게시글 검색 성공",content = @Content(schema = @Schema(implementation = ListResponseDto.class))),
            @ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다. / 검색옵션이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404",description = "존재하지 않는 유저입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/scrap")
    public ResponseEntity<ResponseDto<ListResponseDto>> searchScrap(@AuthenticationPrincipal Member member, @RequestParam @NotBlank(message = "공백일 수 없습니다.") @Size(min = 2,message = "2글자 이상 입력해야 합니다.") String query, @RequestParam(required = false) String sort
            , @RequestParam(required = false,defaultValue = "1") @Min(1) int page){
        return ResponseEntity.ok(ResponseDto.of(postService.searchInScrap(member,query,page,sort),"스크랩 게시글 검색 성공"));
    }

    @Operation(summary = "스크랩 폴더에서 게시글 검색",description = "url 경로에 폴더의 id, url 파라미터에 검색내용 query , 정렬기준을 sort(date/공백(최신순),like,scrap)를, 페이지(공백일 시 1)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "스크랩 폴더에서 게시글 검색 성공",content = @Content(schema = @Schema(implementation = ListResponseDto.class))),
            @ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다. / 검색옵션이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404",description = "존재하지 않는 스크랩폴더입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/folder/{folderId}")
    public ResponseEntity<ResponseDto<ListResponseDto>> searchInFolder(@RequestParam @NotBlank(message = "공백일 수 없습니다.") @Size(min = 2,message = "2글자 이상 입력해야 합니다.") String query, @RequestParam(required = false) String sort
            , @RequestParam(required = false,defaultValue = "1") @Min(1) int page, @PathVariable Long folderId){
        return ResponseEntity.ok(ResponseDto.of(folderService.searchPostInFolder(folderId,query,sort,page),"스크랩 폴더에서 게시글 검색 성공"));
    }
}
