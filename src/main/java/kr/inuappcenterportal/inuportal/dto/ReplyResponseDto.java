package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "댓글 응답 Dto")
@Getter
@NoArgsConstructor
public class ReplyResponseDto {
    @Schema(description = "댓글의 데이터베이스 id 값")
    private Long id;
    @Schema(description = "댓글의 작성자")
    private String writer;
    @Schema(description = "댓글의 내용")
    private String content;
    @Schema(description = "좋아요")
    private int like;
    @Schema(description = "좋아요 여부")
    private Boolean isLiked;
    @Schema(description = "익명 여부")
    private Boolean isAnonymous;
    @Schema(description = "수정/삭제 가능 여부")
    private Boolean hasAuthority;
    @Schema(description = "생성일",example = "yyyy-mm-dd")
    private LocalDate createDate;
    @Schema(description = "수정일",example = "yyyy-mm-dd")
    private LocalDate modifiedDate;
    @Schema(description = "대댓글")
    private List<ReReplyResponseDto> reReplies;

    @Builder
    public ReplyResponseDto(Long id, String writer, String content, int like, LocalDate createDate, LocalDate modifiedDate, List<ReReplyResponseDto> reReplies, Boolean isLiked,Boolean isAnonymous,Boolean hasAuthority){
        this.id = id;
        this.writer =writer;
        this.content =content;
        this.like = like;
        this.createDate = createDate;
        this.modifiedDate =modifiedDate;
        this.reReplies = reReplies;
        this.isLiked = isLiked;
        this.isAnonymous = isAnonymous;
        this.hasAuthority = hasAuthority;
    }

}
