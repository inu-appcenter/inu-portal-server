package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.Post;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Schema(description = "게시글 리스트 응답Dto")
@Getter
@NoArgsConstructor
public class PostListResponseDto {
    @Schema(description = "게시글 데이터베이스 아이디값")
    private Long id;
    @Schema(description = "제목",example = "제목")
    private String title;
    @Schema(description = "카테고리",example = "카테고리")
    private String category;
    @Schema(description = "작성자",example = "작성자")
    private String writer;
    @Schema(description = "내용",example = "내용")
    private String content;
    @Schema(description = "좋아요")
    private int like;
    @Schema(description = "스크랩")
    private int scrap;
    @Schema(description = "이미지수")
    private int imageCount;
    @Schema(description = "생성일",example = "yyyy-mm-dd")
    private LocalDate createDate;
    @Schema(description = "수정일",example = "yyyy-mm-dd")
    private LocalDate modifiedDate;

    @Builder
    private PostListResponseDto(Long id, String title, String category, String writer,String content, LocalDate createDate, LocalDate modifiedDate, int like, int scrap,int imageCount){
        this.id = id;
        this.title = title;
        this.category = category;
        this.writer = writer;
        this.content = content;
        this.createDate = createDate;
        this.modifiedDate = modifiedDate;
        this.like = like;
        this.scrap = scrap;
        this.imageCount = imageCount;
    }

    public static PostListResponseDto of(Post post, String writer){
        return PostListResponseDto.builder()
                .id(post.getId())
                .createDate(post.getCreateDate())
                .modifiedDate(post.getModifiedDate())
                .category(post.getCategory())
                .writer(writer)
                .content((post.getContent().length()>50)?post.getContent().substring(0,50)+"...":post.getContent())
                .title(post.getTitle())
                .like(post.getPostLikes().size())
                .imageCount(post.getImageCount())
                .scrap(post.getScraps().size())
                .build();
    }

}
