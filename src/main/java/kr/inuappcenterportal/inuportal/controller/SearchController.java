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
import kr.inuappcenterportal.inuportal.dto.PostListResponseDto;
import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@Tag(name = "Search",description = "게시글 검색 API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/search")
public class SearchController {
    private final PostService postService;

    @Operation(summary = "게시글 검색",description = "url 파라미터에 검색내용 query , 정렬기준을 sort(보내지 않을 시 최신순,like,view,scrap)를, 페이지(공백일 시 1)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 검색 성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class))),
            @ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다. / 검색옵션이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<List<PostListResponseDto>>> search(@RequestParam @NotBlank(message = "공백일 수 없습니다.") @Size(min = 2,message = "2글자 이상 입력해야 합니다.") String query, @RequestParam(required = false) String sort
    ,@RequestParam(required = false,defaultValue = "1") @Min(1) int page){
        return new ResponseEntity<>(new ResponseDto<>(postService.searchPost(query,sort,page),"게시글 검색 성공"), HttpStatus.OK);
    }
}
