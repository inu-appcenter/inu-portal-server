package kr.inuappcenterportal.inuportal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.config.TokenProvider;
import kr.inuappcenterportal.inuportal.dto.PostDto;
import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Posts",description = "게시글 API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;
    private final TokenProvider tokenProvider;

    @Operation(summary = "게시글 등록",description = "헤더 Auth에 발급받은 토큰을,바디에 {title,content,category}을 json 형식으로 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "게시글 등록 성공")
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.")
    })
    @PostMapping("")
    public ResponseEntity<?> savePost(@RequestHeader("Auth") String token, @Valid@RequestBody PostDto postSaveDto){
        Long id = Long.valueOf(tokenProvider.getUsername(token));
        log.info("게시글 저장 호출 id:{}",id);
        Long postId = postService.save(id,postSaveDto);
        return new ResponseEntity<>(new ResponseDto<>(postId,"게시글 등록 성공"), HttpStatus.CREATED);
    }

    @Operation(summary = "게시글 수정",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 게시글의 id, 바디에 {title,content,category}을 json 형식으로 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 수정 성공")
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 게시글입니다.")
            ,@ApiResponse(responseCode = "403",description = "이 게시글의 수정/삭제에 대한 권한이 없습니다.")
    })
    @PutMapping("/{postId}")
    public ResponseEntity<?> updatePost(@RequestHeader("Auth") String token, @Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH) @PathVariable Long postId, @Valid@RequestBody PostDto postDto){
        Long memberId = Long.valueOf(tokenProvider.getUsername(token));
        log.info("게시글 수정 호출 id:{}",postId);
        postService.update(memberId,postId,postDto);
        return new ResponseEntity<>(new ResponseDto<>(postId,"게시글 수정 성공"), HttpStatus.OK);
    }

    @Operation(summary = "게시글 삭제",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 게시글의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "204",description = "게시글 삭제 성공")
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 게시글입니다.")
            ,@ApiResponse(responseCode = "403",description = "이 게시글의 수정/삭제에 대한 권한이 없습니다.")
    })
    @DeleteMapping("{postId}")
    public ResponseEntity<?> deletePost(@RequestHeader("Auth") String token,@Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH) @PathVariable Long postId){
        Long memberId = Long.valueOf(tokenProvider.getUsername(token));
        log.info("게시글 삭제 호출 id:{}",postId);
        postService.delete(memberId,postId);
        return new ResponseEntity<>(new ResponseDto<>(postId,"게시글 삭제 성공"), HttpStatus.NO_CONTENT);

    }

    @Operation(summary = "게시글 가져오기",description = "url 파라미터에 게시글의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 가져오기 성공")
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 게시글입니다.")
    })
    @GetMapping("{postId}")
    public ResponseEntity<?> getPost(@Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH)@PathVariable Long postId){
        log.info("게시글 가져오기 호출 id:{}",postId);
        return new ResponseEntity<>(postService.getPost(postId),HttpStatus.OK);
    }

    @Operation(summary = "게시글 좋아요",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 게시글의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 좋아요 성공")
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 게시글입니다.")
            ,@ApiResponse(responseCode = "409",description = "이미 좋아요를 누른 게시글입니다.")
    })
    @PostMapping("/like/{postId}")
    public ResponseEntity<?> likePost(@RequestHeader("Auth") String token, @Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH)@PathVariable Long postId){
        Long memberId = Long.valueOf(tokenProvider.getUsername(token));
        log.info("게시글 좋아요 호출 id:{}",postId);
        postService.likePost(memberId,postId);
        return new ResponseEntity<>(new ResponseDto<>(postId,"게시글 좋아요 성공"), HttpStatus.OK);
    }

    @Operation(summary = "게시글 싫어요",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 게시글의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 싫어요 성공")
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 게시글입니다.")
            ,@ApiResponse(responseCode = "409",description = "이미 싫어요를 누른 게시글입니다.")

    })
    @PostMapping("/dislike/{postId}")
    public ResponseEntity<?> dislikePost(@RequestHeader("Auth") String token,@Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH)@PathVariable Long postId){
        Long memberId = Long.valueOf(tokenProvider.getUsername(token));
        log.info("게시글 싫어요 호출 id:{}",postId);
        postService.dislikePost(memberId,postId);
        return new ResponseEntity<>(new ResponseDto<>(postId,"게시글 싫어요 성공"), HttpStatus.OK);
    }





}
