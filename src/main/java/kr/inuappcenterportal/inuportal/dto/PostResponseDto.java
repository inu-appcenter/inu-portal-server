package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.Post;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Schema(description = "게시글 내용 응답Dto")
@Getter
@NoArgsConstructor
public class PostResponseDto {
    @Schema(description = "게시글 데이터베이스 아이디값")
    private Long id;
    @Schema(description = "제목",example = "제목")
    private String title;
    @Schema(description = "카테고리",example = "카테고리")
    private String category;
    @Schema(description = "작성자",example = "작성자")
    private String writer;
    @Schema(description = "작성자의 프로필 횃불이사진Id값")
    private Long fireId;
    @Schema(description = "내용", example = "내용")
    private String content;
    @Schema(description = "좋아요")
    private Long like;
    @Schema(description = "스크랩")
    private Long scrap;
    @Schema(description = "조회수")
    private Long view;
    @Schema(description = "댓글수")
    private Long replyCount;
    @Schema(description = "좋아요 여부",example = "false")
    private Boolean isLiked;
    @Schema(description = "스크랩 여부",example = "false")
    private Boolean isScraped;
    @Schema(description = "수정/삭제 가능 여부")
    private Boolean hasAuthority;
    @Schema(description = "생성일",example = "yyyy-mm-dd")
    private String createDate;
    @Schema(description = "수정일",example = "yyyy-mm-dd")
    private String modifiedDate;
    @Schema(description = "이미지 갯수")
    private Long imageCount;
    @Schema(description = "베스트 댓글")
    private List<ReReplyResponseDto> bestReplies;
    @Schema(description = "댓글",example = "댓글들")
    private List<ReplyResponseDto> replies;


    @Builder
    private PostResponseDto(Long id, String title, String category, List<ReplyResponseDto> replies, List<ReReplyResponseDto> bestReplies,String writer, long fireId,String content, String createDate, String modifiedDate, long like, long scrap,boolean isLiked, boolean isScraped, long view,long imageCount,boolean hasAuthority,long replyCount){
        this.id = id;
        this.title = title;
        this.category = category;
        this.replies = replies;
        this.bestReplies = bestReplies;
        this.writer = writer;
        this.fireId = fireId;
        this.createDate = createDate;
        this.modifiedDate = modifiedDate;
        this.content =content;
        this.like = like;
        this.scrap = scrap;
        this.isLiked = isLiked;
        this.isScraped = isScraped;
        this.view = view;
        this.imageCount = imageCount;
        this.hasAuthority =hasAuthority;
        this.replyCount = replyCount;
    }

    public static PostResponseDto of(Post post,String writer, long fireId, boolean isLiked, boolean isScraped, boolean hasAuthority, List<ReplyResponseDto> replies , List<ReReplyResponseDto> bestReplies){
        return PostResponseDto.builder()
                .id(post.getId())
                .replies(replies)
                .bestReplies(bestReplies)
                .createDate(post.getCreateDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .modifiedDate(post.getModifiedDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .category(post.getCategory())
                .writer(writer)
                .fireId(fireId)
                .title(post.getTitle())
                .content(post.getContent())
                .like(post.getGood())
                .scrap(post.getScrap())
                .isLiked(isLiked)
                .isScraped(isScraped)
                .hasAuthority(hasAuthority)
                .view(post.getView())
                .imageCount(post.getImageCount())
                .replyCount(post.getReplyCount())
                .build();
    }

}
