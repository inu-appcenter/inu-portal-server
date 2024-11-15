package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.Reply;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Schema(description = "댓글 응답 Dto")
@Getter
@NoArgsConstructor
public class ReplyResponseDto {
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
    private String  modifiedDate;
    @Schema(description = "대댓글")
    private List<ReReplyResponseDto> reReplies;

    @Builder
    private ReplyResponseDto(Long id, String writer, String content,long fireId, long like, String createDate, String modifiedDate, List<ReReplyResponseDto> reReplies, boolean isLiked,boolean isAnonymous,boolean hasAuthority){
        this.id = id;
        this.writer =writer;
        this.fireId = fireId;
        this.content =content;
        this.like = like;
        this.createDate = createDate;
        this.modifiedDate =modifiedDate;
        this.reReplies = reReplies;
        this.isLiked = isLiked;
        this.isAnonymous = isAnonymous;
        this.hasAuthority = hasAuthority;
    }

    public static ReplyResponseDto of(Reply reply,String writer, long fireId, boolean isLiked, boolean hasAuthority,List<ReReplyResponseDto> reReplyResponseDtoList){
        return ReplyResponseDto.builder()
                .id(reply.getId())
                .writer(writer)
                .fireId(fireId)
                .content(reply.getContent())
                .like(reply.getLikeReplies().size())
                .createDate(reply.getCreateDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .modifiedDate(reply.getModifiedDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .reReplies(reReplyResponseDtoList)
                .isLiked(isLiked)
                .isAnonymous(reply.getAnonymous())
                .hasAuthority(hasAuthority)
                .build();
    }

}
