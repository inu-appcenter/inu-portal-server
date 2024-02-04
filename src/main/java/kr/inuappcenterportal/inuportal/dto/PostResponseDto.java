package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.Post;
import kr.inuappcenterportal.inuportal.domain.Reply;
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
    @Schema(description = "제목")
    private String title;
    @Schema(description = "카테고리")
    private String category;
    @Schema(description = "작성자")
    private String writer;
    @Schema(description = "좋아요")
    private int good;
    @Schema(description = "싫어요")
    private int bad;
    @Schema(description = "생성일")
    private LocalDate createDate;
    @Schema(description = "수정일")
    private LocalDate modifiedDate;
    @Schema(description = "댓글")
    private List<Reply> replies;

    @Builder
    public PostResponseDto(Post post){
        String email = post.getMember().getEmail();
        int atIndex = email.indexOf('@');
        if (atIndex != -1) {
             email = email.substring(0, atIndex);
        }
        this.id = post.getId();
        this.title = post.getTitle();
        this.category = post.getCategory();
        this.replies = post.getReplies();
        this.writer = email;
        this.createDate = post.getCreateDate();
        this.modifiedDate = post.getModifiedDate();
        this.good = post.getGoods().size();
        this.bad = post.getDisLikes().size();

    }

}
