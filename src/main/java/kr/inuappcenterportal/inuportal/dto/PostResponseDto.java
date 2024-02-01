package kr.inuappcenterportal.inuportal.dto;

import kr.inuappcenterportal.inuportal.domain.Post;
import kr.inuappcenterportal.inuportal.domain.Reply;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class PostResponseDto {
    private Long id;
    private String title;
    private String category;
    private String writer;
    private int good;
    private int bad;
    private LocalDate createDate;
    private LocalDate modifiedDate;
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
