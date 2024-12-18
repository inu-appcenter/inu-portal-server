package kr.inuappcenterportal.inuportal.domain.reply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.reply.model.Reply;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Schema(description = "대댓글 응답 Dto")
@Getter
@NoArgsConstructor
public class ReReplyResponseDto {
    @Schema(description = "댓글의 데이터베이스 id 값")
    private Long id;
    @Schema(description = "댓글의 작성자")
    private String writer;
    @Schema(description = "댓글의 작성자의 프로필 횃불이사진Id값")
    private Long fireId;
    @Schema(description = "댓글의 내용")
    private String content;
    @Schema(description = "좋아요")
    private Long like;
    @Schema(description = "좋아요 여부")
    private Boolean isLiked;
    @Schema(description = "익명 여부")
    private Boolean isAnonymous;
    @Schema(description = "수정/삭제 가능 여부")
    private Boolean hasAuthority;
    @Schema(description = "생성일",example = "yyyy-mm-dd")
    private String createDate;
    @Schema(description = "수정일",example = "yyyy-mm-dd")
    private String modifiedDate;

    @Builder
    private ReReplyResponseDto(Long id, String writer, long fireId, String content, long like, String createDate, String modifiedDate,boolean isLiked, boolean isAnonymous, boolean hasAuthority ){
        this.id = id;
        this.writer =writer;
        this.fireId = fireId;
        this.content = content;
        this.like = like;
        this.createDate =createDate;
        this.modifiedDate = modifiedDate;
        this.isLiked =isLiked;
        this.isAnonymous = isAnonymous;
        this.hasAuthority = hasAuthority;
    }

    public static ReReplyResponseDto of(Reply reReply, String writer,long fireId, boolean isLiked, boolean hasAuthority)
    {
        return ReReplyResponseDto.builder()
                .id(reReply.getId())
                .writer(writer)
                .fireId(fireId)
                .content(reReply.getIsDeleted()?"삭제된 댓글입니다.":reReply.getContent())
                .like(reReply.getGood())
                .createDate(reReply.getCreateDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .modifiedDate(reReply.getModifiedDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .isLiked(isLiked)
                .hasAuthority(hasAuthority)
                .isAnonymous(reReply.getAnonymous())
                .build();
    }





}
