package kr.inuappcenterportal.inuportal.domain.post.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.post.dto.CategoryPostResponseDto;
import kr.inuappcenterportal.inuportal.domain.post.dto.PostDto;
import kr.inuappcenterportal.inuportal.domain.post.dto.PostListResponseDto;
import kr.inuappcenterportal.inuportal.domain.post.dto.PostResponseDto;
import kr.inuappcenterportal.inuportal.domain.post.service.PostService;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
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
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@Validated
public class PostController implements PostApiSpecification {
    private final PostService postService;


    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Long>> savePost(@Valid @RequestPart PostDto postDto, @AuthenticationPrincipal Member member, @RequestPart(required = false) List<MultipartFile> images) throws NoSuchAlgorithmException, IOException {
        log.info("게시글만 저장 호출 id:{}", member.getId());
        Long postId = postService.savePost(member, postDto, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.of(postId, "게시글 등록 성공"));
    }

    @PutMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Long>> updatePost(@AuthenticationPrincipal Member member, @PathVariable Long postId, @Valid @RequestPart PostDto postDto, @RequestPart(required = false) List<MultipartFile> images) throws IOException {
        log.info("게시글 수정 호출 id:{}", postId);
        postService.updatePost(member.getId(), postId, postDto, images);
        return ResponseEntity.ok(ResponseDto.of(postId, "게시글 수정 성공"));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ResponseDto<Long>> deletePost(@AuthenticationPrincipal Member member, @PathVariable Long postId) throws IOException {
        log.info("게시글 삭제 호출 id:{}", postId);
        postService.delete(member, postId);
        return ResponseEntity.ok(ResponseDto.of(postId, "게시글 삭제 성공"));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ResponseDto<PostResponseDto>> getPost(@AuthenticationPrincipal Member member, HttpServletRequest httpServletRequest, @PathVariable Long postId) {
        //log.info("게시글 가져오기 호출 id:{}",postId);
        return ResponseEntity.ok(ResponseDto.of(postService.getPost(postId, member, httpServletRequest.getHeader("X-Forwarded-For")), "게시글 가져오기 성공"));
    }

    @PutMapping("/{postId}/like")
    public ResponseEntity<ResponseDto<Integer>> likePost(@AuthenticationPrincipal Member member, @PathVariable Long postId) {
        log.info("게시글 좋아요 여부 변경 호출 id:{}", postId);
        return ResponseEntity.ok(ResponseDto.of(postService.likePost(member, postId), "게시글 좋아요 여부 변경성공"));
    }


    @PutMapping("/{postId}/scrap")
    public ResponseEntity<ResponseDto<Integer>> scrapPost(@AuthenticationPrincipal Member member, @PathVariable Long postId) {
        log.info("게시글 스크랩 여부 변경 호출 id:{}", postId);
        return ResponseEntity.ok(ResponseDto.of(postService.scrapPost(member, postId), "스크랩 여부 변경 성공"));
    }

    @GetMapping("/categories")
    public ResponseEntity<ResponseDto<List<CategoryPostResponseDto>>> getPostsByCategory(@RequestParam(required = false, defaultValue = "5") int count, @AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(ResponseDto.of(postService.getPostsByCategories(count, member), "카테고리별 게시글 가져오기 성공"));
    }

    @GetMapping("")
    public ResponseEntity<ResponseDto<ListResponseDto<PostListResponseDto>>> getAllPost(@RequestParam(required = false) String category, @RequestParam(required = false, defaultValue = "random") String sort
            , @RequestParam(required = false, defaultValue = "1") @Min(1) int page, @AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(ResponseDto.of(postService.getAllPost(category, sort, page, member), "모든 게시글 가져오기 성공"));
    }

    @GetMapping("/{postId}/images/{imageId}")
    public ResponseEntity<byte[]> getImages(@PathVariable Long postId, @PathVariable Long imageId) throws IOException {
        //log.info("게시글의 이미지 가져오기 호출 id:{}",postId);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.valueOf("image/webp"));
        return ResponseEntity.ok().headers(httpHeaders).body(postService.getPostImage(postId, imageId));
    }

    @GetMapping("/top")
    public ResponseEntity<ResponseDto<List<PostListResponseDto>>> getPostForTop(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(ResponseDto.of(postService.getTop(category), "인기 게시글 가져오기 성공"));
    }

    @GetMapping("/main")
    public ResponseEntity<ResponseDto<List<PostListResponseDto>>> getPostForMain() {
        return ResponseEntity.ok(ResponseDto.of(postService.getRandomTop(), "메인 페이지 게시글 7개 가져오기 성공"));
    }

    @GetMapping("/mobile")
    public ResponseEntity<ResponseDto<List<PostListResponseDto>>> getPostForMobile(@RequestParam(required = false) Long lastPostId, @RequestParam(required = false) String category, @AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(ResponseDto.of(postService.getPostForInf(lastPostId, category, member), "모바일용 게시글 리스트 가져오기 성공"));
    }
}
