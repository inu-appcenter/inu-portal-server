package kr.inuappcenterportal.inuportal.controller;

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
import kr.inuappcenterportal.inuportal.config.TokenProvider;
import kr.inuappcenterportal.inuportal.dto.PostDto;
import kr.inuappcenterportal.inuportal.dto.PostListResponseDto;
import kr.inuappcenterportal.inuportal.dto.PostResponseDto;
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

    @Operation(summary = "게시글 등록",description = "헤더 Auth에 발급받은 토큰을,바디에 {title,content,category,bool 형태의 anonymous}을 json 형식으로 보내주세요. 성공 시 작성된 게시글의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "게시글 등록 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("")
    public ResponseEntity<?> savePost(/*@RequestHeader("Auth") String token,*/ @Valid@RequestBody PostDto postSaveDto, HttpServletRequest httpServletRequest){
        Long id = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        log.info("게시글 저장 호출 id:{}",id);
        Long postId = postService.save(id,postSaveDto);
        return new ResponseEntity<>(new ResponseDto<>(postId,"게시글 등록 성공"), HttpStatus.CREATED);
    }

    @Operation(summary = "게시글 수정",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 게시글의 id, 바디에 {title,content,category, bool 형태의 anonymous}을 json 형식으로 보내주세요. 성공 시 수정된 게시글의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 수정 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 게시글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "403",description = "이 게시글의 수정/삭제에 대한 권한이 없습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping("/{postId}")
    public ResponseEntity<?> updatePost(/*@RequestHeader("Auth") String token,*/HttpServletRequest httpServletRequest, @Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH) @PathVariable Long postId, @Valid@RequestBody PostDto postDto){
        Long memberId = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        log.info("게시글 수정 호출 id:{}",postId);
        postService.update(memberId,postId,postDto);
        return new ResponseEntity<>(new ResponseDto<>(postId,"게시글 수정 성공"), HttpStatus.OK);
    }

    @Operation(summary = "게시글 삭제",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 게시글의 id를 보내주세요. 성공 시 삭제된 게시글의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 삭제 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 게시글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "403",description = "이 게시글의 수정/삭제에 대한 권한이 없습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(/*@RequestHeader("Auth") String token,*/HttpServletRequest httpServletRequest, @Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH) @PathVariable Long postId){
        Long memberId = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        log.info("게시글 삭제 호출 id:{}",postId);
        postService.delete(memberId,postId);
        return new ResponseEntity<>(new ResponseDto<>(postId,"게시글 삭제 성공"), HttpStatus.OK);

    }

    @Operation(summary = "게시글 가져오기",description = "로그인 한 상태면 헤더에 Auth에 발급받은 토큰을 담아서 url 파라미터에 게시글의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 가져오기 성공",content = @Content(schema = @Schema(implementation = PostResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 게시글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/{postId}")
    public ResponseEntity<?> getPost(/*@RequestHeader(name = "Auth",required = false) String token*/ HttpServletRequest httpServletRequest, @Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH)@PathVariable Long postId){
        log.info("게시글 가져오기 호출 id:{}",postId);
        Long memberId = -1L;
        if(httpServletRequest.getHeader("Auth")!=null){
            memberId = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        }
        return new ResponseEntity<>(postService.getPost(postId,memberId),HttpStatus.OK);
    }

    @Operation(summary = "게시글 좋아요 여부 변경",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 게시글의 id를 보내주세요. 좋아요 시 {data:1}, 좋아요 취소 시 {data:-1}입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 좋아요 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 게시글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping("/like/{postId}")
    public ResponseEntity<?> likePost(/*@RequestHeader("Auth") String token*/HttpServletRequest httpServletRequest, @Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH)@PathVariable Long postId){
        Long memberId = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        log.info("게시글 좋아요 여부 변경 호출 id:{}",postId);
        return new ResponseEntity<>(new ResponseDto<>(postService.likePost(memberId,postId),"게시글 좋아요 여부 변경성공"), HttpStatus.OK);
    }


    @Operation(summary = "스크랩 여부 변경",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 게시글의 id를 보내주세요. 스크랩 시 {data:1}, 스크랩 해제 시 {data:-1} 입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 스크랩 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 게시글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping("/scrap/{postId}")
    public ResponseEntity<?> scrapPost(/*@RequestHeader("Auth") String token*/HttpServletRequest httpServletRequest, @Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH)@PathVariable Long postId){
        Long memberId = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        log.info("게시글 스크랩 여부 변경 호출 id:{}",postId);
        return new ResponseEntity<>(new ResponseDto<>(postService.scrapPost(memberId,postId),"스크랩 여부 변경 성공"),HttpStatus.OK);
    }

    @Operation(summary = "모든 게시글 가져오기",description = "모든 게시글은 최신순으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "모든 게시글 가져오기 성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))}
            )
    @GetMapping("/all")
    public ResponseEntity<?> getAllPost(){
        return new ResponseEntity<>(new ResponseDto<>(postService.getAllPost(),"모든 게시글 가져오기 성공"),HttpStatus.OK);
    }

    @Operation(summary = "카테고리별 모든 게시글 가져오기",description = "url 파라미터에 카테고리를 보내주세요. 게시글은 좋아요 순으로 정렬되어 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "카테고리별 모든 게시글 가져오기 성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 카테고리입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))}
    )
    @GetMapping("/all/{category}")
    public ResponseEntity<?> getAllPost(@PathVariable String category){
        return new ResponseEntity<>(new ResponseDto<>(postService.getPostByCategory(category),"모든 게시글 가져오기 성공"),HttpStatus.OK);
    }






}
