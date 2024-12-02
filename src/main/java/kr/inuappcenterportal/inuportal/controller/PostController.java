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
import jakarta.validation.constraints.Min;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.dto.*;
import kr.inuappcenterportal.inuportal.service.PostService;
import kr.inuappcenterportal.inuportal.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Tag(name = "Posts",description = "게시글 API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/posts")
@Validated
public class PostController {
    private final PostService postService;
    private final RedisService redisService;


    @Operation(summary = "게시글 등록",description = "헤더 Auth에 발급받은 토큰을, 바디에 {title,content,category,bool 형태의 anonymous} 보내주세요. 그 이후 등록된 게시글의 id와 이미지를 보내주세요. 성공 시 작성된 게시글의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "게시글 등록 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "일정 시간 동안 같은 게시글이나 댓글을 작성할 수 없습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("")
    public ResponseEntity<ResponseDto<Long>> saveOnlyPost(@Valid@RequestBody PostDto postSaveDto, @AuthenticationPrincipal Member member) throws NoSuchAlgorithmException {
        log.info("게시글만 저장 호출 id:{}",member.getId());
        Long postId = postService.saveOnlyPost(member,postSaveDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.of(postId,"게시글 등록 성공"));
    }

    @Operation(summary = "이미지 등록",description = "파라미터에 게시글의 id, images에 이미지 파일들을 보내주세요. 성공 시 게시글의 데이터베이 아이디값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "이미지 등록 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 게시글입니다. / 존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping(value = "/{postId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Long>> saveOnlyImage( @RequestPart List<MultipartFile> images,@PathVariable Long postId, @AuthenticationPrincipal Member member) throws IOException {
        log.info("이미지만 저장 호출 id:{}",postId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.of(postService.saveImageLocal(member,postId,images),"이미지 등록 성공"));
    }


    @Operation(summary = "게시글 수정",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 게시글의 id, 바디에 {title,content,category, bool 형태의 anonymous}을 형식으로 보내주세요. 성공 시 수정된 게시글의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 수정 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다. / 존재하지 않는 게시글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "403",description = "이 게시글의 수정/삭제에 대한 권한이 없습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping("/{postId}")
    public ResponseEntity<ResponseDto<Long>> updateOnlyPost(@AuthenticationPrincipal Member member, @Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH) @PathVariable Long postId, @Valid@RequestBody PostDto postDto) {
        log.info("게시글 수정 호출 id:{}",postId);
        postService.updateOnlyPost(member.getId(),postId,postDto);
        return ResponseEntity.ok(ResponseDto.of(postId,"게시글 수정 성공"));
    }
    @Operation(summary = "게시글의 이미지 수정",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 게시글의 id, images 에 기존 이미지를 포함한 이미지들을 보내주세요.(아무 이미지도 보내지 않을 시 모든 이미지가 삭제됩니다) 성공 시 수정된 게시글의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글이미지 수정 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다. / 존재하지 않는 게시글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "403",description = "이 게시글의 수정/삭제에 대한 권한이 없습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping(value = "/{postId}/images",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Long>> updateOnlyImage(@AuthenticationPrincipal Member member, @Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH) @PathVariable Long postId, @RequestPart(required = false) List<MultipartFile> images) throws IOException {
        log.info("게시글 수정 호출 id:{}",postId);
        if(images==null){
            images = new ArrayList<>();
        }
        postService.updateImageLocal(member.getId(),postId,images);
        return ResponseEntity.ok(ResponseDto.of(postId,"게시글 수정 성공"));
    }

    @Operation(summary = "게시글 삭제",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 게시글의 id를 보내주세요. 성공 시 삭제된 게시글의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 삭제 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다. / 존재하지 않는 게시글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "403",description = "이 게시글의 수정/삭제에 대한 권한이 없습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<ResponseDto<Long>> deletePost(@AuthenticationPrincipal Member member, @Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH) @PathVariable Long postId) throws IOException {
        log.info("게시글 삭제 호출 id:{}",postId);
        postService.delete(member.getId(),postId);
        return ResponseEntity.ok(ResponseDto.of(postId,"게시글 삭제 성공"));

    }

    @Operation(summary = "게시글 가져오기",description = "로그인 한 상태면 헤더에 Auth에 발급받은 토큰을 담아서 url 파라미터에 게시글의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 가져오기 성공",content = @Content(schema = @Schema(implementation = PostResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 게시글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/{postId}")
    public ResponseEntity<ResponseDto<PostResponseDto>> getPost(@AuthenticationPrincipal Member member, HttpServletRequest httpServletRequest, @Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH)@PathVariable Long postId){
        log.info("게시글 가져오기 호출 id:{}",postId);
        return ResponseEntity.ok(ResponseDto.of(postService.getPost(postId,member,httpServletRequest.getHeader("X-Forwarded-For")),"게시글 가져오기 성공"));
    }

    @Operation(summary = "게시글 좋아요 여부 변경",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 게시글의 id를 보내주세요. 좋아요 시 {data:1}, 좋아요 취소 시 {data:-1}입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 좋아요 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다. / 존재하지 않는 게시글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "자신의 게시글에는 추천을 할 수 없습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))

    })
    @PutMapping("/{postId}/like")
    public ResponseEntity<ResponseDto<Integer>> likePost(@AuthenticationPrincipal Member member, @Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH)@PathVariable Long postId){
        log.info("게시글 좋아요 여부 변경 호출 id:{}",postId);
        return ResponseEntity.ok(ResponseDto.of(postService.likePost(member,postId),"게시글 좋아요 여부 변경성공"));
    }


    @Operation(summary = "스크랩 여부 변경",description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 게시글의 id를 보내주세요. 스크랩 시 {data:1}, 스크랩 해제 시 {data:-1} 입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 스크랩 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다. / 존재하지 않는 게시글입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping("/{postId}/scrap")
    public ResponseEntity<ResponseDto<Integer>> scrapPost(@AuthenticationPrincipal Member member, @Parameter(name = "postId",description = "게시글의 id",in = ParameterIn.PATH)@PathVariable Long postId){
        log.info("게시글 스크랩 여부 변경 호출 id:{}",postId);
        return ResponseEntity.ok(ResponseDto.of(postService.scrapPost(member,postId),"스크랩 여부 변경 성공"));
    }

    @Operation(summary = "모든 게시글 가져오기",description = "카테고리(공백일 시 모든 게시글), 정렬기준 sort(date/공백(최신순), like, scrap), 페이지(공백일 시 1)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "모든 게시글 가져오기 성공",content = @Content(schema = @Schema(implementation = ListResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<ListResponseDto>> getAllPost(@RequestParam(required = false) String category, @RequestParam(required = false,defaultValue = "date") String sort
            ,@RequestParam(required = false,defaultValue = "1") @Min(1) int page ){
        return ResponseEntity.ok(ResponseDto.of(postService.getAllPost(category, sort,page),"모든 게시글 가져오기 성공"));
    }


    @Operation(summary = "게시글의 이미지 가져오기",description = "url 파라미터에 postId, imageId를 보내주세요. imageId는 이미지의 등록 순번입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "게시글 가져오기 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 게시글입니다. / 존재하지 않는 이미지 번호입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/{postId}/images/{imageId}")
    public ResponseEntity<byte[]> getImages(@PathVariable Long postId, @PathVariable Long imageId) throws IOException {
        log.info("게시글의 이미지 가져오기 호출 id:{}",postId);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.IMAGE_PNG);
        return ResponseEntity.ok().headers(httpHeaders).body(postService.getImage(postId, imageId));
    }

    @Operation(summary = "상단부 인기 게시글 12개 가져오기",description = "기본 호출 시 모든 글에 대한 인기 게시글 12개, 파라미터로 category를 보낼 시 카테고리의 인기글 12개가 호출됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "인기 게시글 가져오기 성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
    })
    @GetMapping("/top")
    public ResponseEntity<ResponseDto<List<PostListResponseDto>>> getPostForTop(@RequestParam(required = false) String category){
        return ResponseEntity.ok(ResponseDto.of(postService.getTop(category),"인기 게시글 가져오기 성공"));
    }

    @Operation(summary = "메인 페이지 게시글 7개 가져오기",description = "좋아요가 1개 이상인 게시글 9개의 리스트가 3시간 간격으로 랜덤으로 바뀝니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "메인 페이지 게시글 7개 가져오기 성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
    })
    @GetMapping("/main")
    public ResponseEntity<ResponseDto<List<PostListResponseDto>>> getPostForMain(){
        return ResponseEntity.ok(ResponseDto.of(postService.getRandomTop(),"메인 페이지 게시글 7개 가져오기 성공"));
    }

    @Operation(summary = "모바일용 게시글 리스트 가져오기",description = "이전 페이지의 마지막 게시글의 id값을 파라미터로(첫 페이지는 보내지마세요), 카테고리가 없을 시 전체입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "모바일용 게시글 리스트 가져오기 성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
    })
    @GetMapping("/mobile")
    public ResponseEntity<ResponseDto<List<PostListResponseDto>>> getPostForMobile(@RequestParam(required = false) Long lastPostId, @RequestParam(required = false) String category){
        return ResponseEntity.ok(ResponseDto.of(postService.getPostForInf(lastPostId,category),"모바일용 게시글 리스트 가져오기 성공"));
    }

}
