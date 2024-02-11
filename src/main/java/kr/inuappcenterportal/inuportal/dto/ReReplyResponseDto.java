package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.Reply;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
@Schema(description = "대댓글 응답 Dto")
@Getter
@NoArgsConstructor
public class ReReplyResponseDto {
    @Schema(description = "댓글의 데이터베이스 id 값")
    private Long id;
    @Schema(description = "댓글의 작성자")
    private String writer;
    @Schema(description = "댓글의 내용")
    private String content;
    @Schema(description = "좋아요")
    private int like;
    @Schema(description = "좋아요 여부")
    private Boolean isLike;
    @Schema(description = "생성일",example = "yyyy-mm-dd")
    private LocalDate createDate;
    @Schema(description = "수정일",example = "yyyy-mm-dd")
    private LocalDate modifiedDate;

    @Builder
    public ReReplyResponseDto(Long id, String writer, String content, int like, LocalDate createDate, LocalDate modifiedDate,Boolean isLike){
        this.id = id;
        this.writer =writer;
        this.content = content;
        this.like = like;
        this.createDate =createDate;
        this.modifiedDate = modifiedDate;
        this.isLike =isLike;
    }


}
