package kr.inuappcenterportal.inuportal.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.dto.PostListResponseDto;
import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Search",description = "게시글 검색 API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/search")
public class SearchController {
    private final PostService postService;

    @Operation(summary = "게시글 검색",description = "url 파라미터에 검색내용 query , 검색옵션 option(title,content,writer), 정렬기준을 sort(보내지 않을 시 최신순,like,view,scrap) 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 검색 성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class))),
            @ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "400",description = "검색옵션이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/{query}")
    public ResponseEntity<ResponseDto<List<PostListResponseDto>>> search(@PathVariable String query, @RequestParam String option, @RequestParam(required = false) String sort ){
        return new ResponseEntity<>(new ResponseDto<>(postService.searchPost(query,option,sort),"게시글 검색 성공"), HttpStatus.OK);
    }
}