package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.Report;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Schema(description = "신고 내용 응답 Dto")
@Getter
@NoArgsConstructor
public class ReportResponseDto {
    @Schema(description = "신고의 데이터베이스 id 값")
    private Long id;
    @Schema(description = "신고사유")
    private String reason;
    @Schema(description = "신고 코멘트")
    private String comment;
    @Schema(description = "신고자 id")
    private Long memberId;
    @Schema(description = "신고된 게시글 id")
    private Long postId;
    @Schema(description = "신고한 날짜")
    private String createDate;

    @Builder
    private ReportResponseDto (Long id, String reason, String comment, Long memberId, Long postId,String createDate){
        this.id = id;
        this.reason = reason;
        this.comment = comment;
        this.memberId = memberId;
        this.postId = postId;
        this.createDate = createDate;
    }

    public static ReportResponseDto of(Report report){
        return ReportResponseDto.builder()
                .id(report.getId())
                .reason(report.getReason())
                .comment(report.getComment())
                .memberId(report.getMemberId())
                .postId(report.getPostId())
                .createDate(report.getCreateDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .build();
    }

}
