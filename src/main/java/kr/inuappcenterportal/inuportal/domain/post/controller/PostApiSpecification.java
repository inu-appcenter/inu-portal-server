package kr.inuappcenterportal.inuportal.domain.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.post.dto.CategoryPostResponseDto;
import kr.inuappcenterportal.inuportal.domain.post.dto.PostDto;
import kr.inuappcenterportal.inuportal.domain.post.dto.PostListResponseDto;
import kr.inuappcenterportal.inuportal.domain.post.dto.PostResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Tag(name = "Posts", description = "게시글 API")
public interface PostApiSpecification {

    @Operation(summary = "게시글 등록", description = "헤더 Auth에 발급받은 토큰을, 바디에 {title,content,category,bool 형태의 anonymous} 보내주세요. 그 이후 등록된 게시글의 id와 이미지를 보내주세요. 성공 시 작성된 게시글의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "게시글 등록 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "400", description = "일정 시간 동안 같은 게시글이나 댓글을 작성할 수 없습니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "404", description = "존재하지 않는 회원입니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<ResponseDto<Long>> savePost(
            @Parameter(
                    description = "게시글 정보(JSON)",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PostDto.class))
            )
            @Valid @RequestPart PostDto postDto,
            @AuthenticationPrincipal Member member,
            @Parameter(
                    description = "첨부 이미지 파일들",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, array = @ArraySchema(schema = @Schema(type = "string", format = "binary")))
            )
            @RequestPart(required = false) List<MultipartFile> images
    ) throws NoSuchAlgorithmException, IOException;

    @Operation(summary = "게시글 수정", description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 게시글의 id, 바디에 {title,content,category, bool 형태의 anonymous}을 형식으로 보내주세요. 성공 시 수정된 게시글의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 수정 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "404", description = "존재하지 않는 회원입니다. / 존재하지 않는 게시글입니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "403", description = "이 게시글의 수정/삭제에 대한 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<ResponseDto<Long>> updatePost(
            @AuthenticationPrincipal Member member,
            @Parameter(name = "postId", description = "게시글의 id", in = ParameterIn.PATH) @PathVariable Long postId,
            @Parameter(
                    description = "게시글 정보(JSON)",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PostDto.class))
            )
            @Valid @RequestPart PostDto postDto,
            @Parameter(
                    description = "첨부 이미지 파일들",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, array = @ArraySchema(schema = @Schema(type = "string", format = "binary")))
            )
            @RequestPart(required = false) List<MultipartFile> images
    ) throws IOException;

    @Operation(summary = "게시글 삭제", description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 게시글의 id를 보내주세요. 성공 시 삭제된 게시글의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 삭제 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "404", description = "존재하지 않는 회원입니다. / 존재하지 않는 게시글입니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "403", description = "이 게시글의 수정/삭제에 대한 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<ResponseDto<Long>> deletePost(
            @AuthenticationPrincipal Member member,
            @Parameter(name = "postId", description = "게시글의 id", in = ParameterIn.PATH) @PathVariable Long postId
    ) throws IOException;

    @Operation(summary = "게시글 가져오기", description = "로그인 한 상태면 헤더에 Auth에 발급받은 토큰을 담아서 url 파라미터에 게시글의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 가져오기 성공", content = @Content(schema = @Schema(implementation = PostResponseDto.class)))
            , @ApiResponse(responseCode = "404", description = "존재하지 않는 게시글입니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<ResponseDto<PostResponseDto>> getPost(
            @AuthenticationPrincipal Member member,
            HttpServletRequest httpServletRequest,
            @Parameter(name = "postId", description = "게시글의 id", in = ParameterIn.PATH) @PathVariable Long postId
    );

    @Operation(summary = "게시글 좋아요 여부 변경", description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 게시글의 id를 보내주세요. 좋아요 시 {data:1}, 좋아요 취소 시 {data:-1}입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 좋아요 여부 변경성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "404", description = "존재하지 않는 회원입니다. / 존재하지 않는 게시글입니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "400", description = "자신의 게시글에는 추천을 할 수 없습니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))

    })
    ResponseEntity<ResponseDto<Integer>> likePost(
            @AuthenticationPrincipal Member member,
            @Parameter(name = "postId", description = "게시글의 id", in = ParameterIn.PATH) @PathVariable Long postId
    );

    @Operation(summary = "스크랩 여부 변경", description = "헤더 Auth에 발급받은 토큰을, url 파라미터에 게시글의 id를 보내주세요. 스크랩 시 {data:1}, 스크랩 해제 시 {data:-1} 입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 스크랩 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "404", description = "존재하지 않는 회원입니다. / 존재하지 않는 게시글입니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<ResponseDto<Integer>> scrapPost(
            @AuthenticationPrincipal Member member,
            @Parameter(name = "postId", description = "게시글의 id", in = ParameterIn.PATH) @PathVariable Long postId
    );

    @Operation(summary = "카테고리별 게시글 가져오기", description = "각 카테고리별로 최신 게시글을 지정된 개수만큼 가져옵니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "카테고리별 게시글 가져오기 성공", content = @Content(schema = @Schema(implementation = CategoryPostResponseDto.class)))
    })
    ResponseEntity<ResponseDto<List<CategoryPostResponseDto>>> getPostsByCategory(
            @RequestParam(required = false, defaultValue = "5") int count,
            @AuthenticationPrincipal Member member
    );

    @Operation(summary = "모든 게시글 가져오기", description = "카테고리(공백일 시 하루마다 랜덤으로 정렬된 모든 게시글), 정렬기준 sort(date, like, scrap), 페이지(공백일 시 1)를 보내주세요. 로그인 한 상태라면 토큰도 보내주세요. <br><br>category: 자유게시판, 수강신청, 장학금, 학산도서관, 기숙사")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "모든 게시글 가져오기 성공", content = @Content(schema = @Schema(implementation = ListResponseDto.class)))
            , @ApiResponse(responseCode = "400", description = "정렬의 기준값이 올바르지 않습니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<ResponseDto<ListResponseDto<PostListResponseDto>>> getAllPost(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "random") String sort,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @AuthenticationPrincipal Member member
    );

    @Operation(summary = "게시글의 이미지 가져오기", description = "url 파라미터에 postId, imageId를 보내주세요. imageId는 이미지의 등록 순번입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글의 이미지 가져오기 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            , @ApiResponse(responseCode = "404", description = "존재하지 않는 게시글입니다. / 존재하지 않는 이미지 번호입니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<byte[]> getImages(
            @Parameter(name = "postId", description = "게시글의 id", in = ParameterIn.PATH) @PathVariable Long postId,
            @Parameter(name = "imageId", description = "이미지 번호", in = ParameterIn.PATH) @PathVariable Long imageId
    ) throws IOException;

    @Operation(summary = "상단부 인기 게시글 12개 가져오기", description = "기본 호출 시 모든 글에 대한 인기 게시글 12개, 파라미터로 category를 보낼 시 카테고리의 인기글 12개가 호출됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인기 게시글 가져오기 성공", content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
    })
    ResponseEntity<ResponseDto<List<PostListResponseDto>>> getPostForTop(
            @RequestParam(required = false) String category
    );

    @Operation(summary = "메인 페이지 게시글 7개 가져오기", description = "좋아요가 1개 이상인 게시글 9개의 리스트가 3시간 간격으로 랜덤으로 바뀝니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메인 페이지 게시글 7개 가져오기 성공", content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
    })
    ResponseEntity<ResponseDto<List<PostListResponseDto>>> getPostForMain();

    @Operation(summary = "모바일용 게시글 리스트 가져오기", description = "이전 페이지의 마지막 게시글의 id값을 파라미터로(첫 페이지는 보내지마세요), 카테고리가 없을 시 전체입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "모바일용 게시글 리스트 가져오기 성공", content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
    })
    ResponseEntity<ResponseDto<List<PostListResponseDto>>> getPostForMobile(
            @RequestParam(required = false) Long lastPostId,
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal Member member
    );
}
