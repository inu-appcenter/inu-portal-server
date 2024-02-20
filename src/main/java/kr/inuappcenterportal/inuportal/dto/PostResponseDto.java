package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
    @Schema(description = "내용", example = "내용")
    private String content;
    @Schema(description = "좋아요")
    private int like;
    @Schema(description = "스크랩")
    private int scrap;
    @Schema(description = "조회수")
    private Long view;
    @Schema(description = "좋아요 여부",example = "false")
    private Boolean isLiked;
    @Schema(description = "스크랩 여부",example = "false")
    private Boolean isScraped;
    @Schema(description = "생성일",example = "yyyy-mm-dd")
    private LocalDate createDate;
    @Schema(description = "수정일",example = "yyyy-mm-dd")
    private LocalDate modifiedDate;
    @Schema(description = "이미지 갯수")
    private Integer imageCount;

    @Schema(description = "댓글",example = "댓글들")
    private List<ReplyResponseDto> replies;


    @Builder
    public PostResponseDto(Long id, String title, String category, List<ReplyResponseDto> replies, String writer, String content, LocalDate createDate, LocalDate modifiedDate, int like, int scrap,Boolean isLiked, Boolean isScraped, Long view,Integer imageCount){
        this.id = id;
        this.title = title;
        this.category = category;
        this.replies = replies;
        this.writer = writer;
        this.createDate = createDate;
        this.modifiedDate = modifiedDate;
        this.content =content;
        this.like = like;
        this.scrap = scrap;
        this.isLiked = isLiked;
        this.isScraped = isScraped;
        this.view = view;
        this.imageCount = imageCount;
    }

}
