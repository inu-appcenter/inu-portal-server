package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "좋아요")
    private int like;
    @Schema(description = "스크랩")
    private int scrap;
    @Schema(description = "생성일",example = "yyyy-mm-dd")
    private LocalDate createDate;
    @Schema(description = "수정일",example = "yyyy-mm-dd")
    private LocalDate modifiedDate;

    @Builder
    public PostListResponseDto(Long id, String title, String category, String writer, LocalDate createDate, LocalDate modifiedDate, int like, int scrap){
        this.id = id;
        this.title = title;
        this.category = category;
        this.writer = writer;
        this.createDate = createDate;
        this.modifiedDate = modifiedDate;
        this.like = like;
        this.scrap = scrap;
    }

}
