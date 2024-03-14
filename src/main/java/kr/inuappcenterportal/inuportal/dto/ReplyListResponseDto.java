package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.Reply;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Schema(description = "회원이 작성한 댓글 리스트 응답 Dto")
@Getter
@NoArgsConstructor
public class ReplyListResponseDto {
    @Schema(description = "댓글의 데이터베이스 id 값")
    private Long id;
    @Schema(description = "댓글의 내용")
    private String content;
    @Schema(description = "좋아요")
    private Long like;
    @Schema(description = "이 댓글이 달린 게시글의 데이터베이스 id값")
    private Long postId;
    @Schema(description = "생성일",example = "yyyy-mm-dd")
    private LocalDate createDate;
    @Schema(description = "수정일",example = "yyyy-mm-dd")
    private LocalDate modifiedDate;

    @Builder
    private ReplyListResponseDto(Reply reply){
        this.id = reply.getId();
        this.content = reply.getContent();
        this.like = (long)reply.getLikeReplies().size();
        this.postId = reply.getPost().getId();
        this.createDate = reply.getCreateDate();
        this.modifiedDate = reply.getModifiedDate();
    }

    public static ReplyListResponseDto of(Reply reply){
        return ReplyListResponseDto.builder().reply(reply).build();
    }
}
