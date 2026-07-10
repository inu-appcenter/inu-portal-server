package kr.inuappcenterportal.inuportal.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.notice.model.Notice;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "학교 공지사항 목록 (본문 포함) 응답 Dto")
@Getter
@NoArgsConstructor
public class NoticeWithContentResponseDto {

    @Schema(description = "공지사항 데이터베이스 id 값")
    private Long id;

    @Schema(description = "카테고리", example = "학사")
    private String category;

    @Schema(description = "세부 카테고리", example = "장학금")
    private String subCategory;

    @Schema(description = "제목", example = "제목")
    private String title;

    @Schema(description = "작성자", example = "작성자")
    private String writer;

    @Schema(description = "작성일", example = "2026.07.08")
    private String createDate;

    @Schema(description = "링크 url", example = "url")
    private String url;

    @Schema(description = "본문 순수 텍스트")
    private String contentText;

    @Schema(description = "첨부파일 목록")
    private List<AttachmentMeta> attachments;

    @Builder
    private NoticeWithContentResponseDto(Notice notice, List<AttachmentMeta> attachments) {
        this.id = notice.getId();
        this.category = notice.getCategory();
        this.subCategory = notice.getSubCategory();
        this.title = notice.getTitle();
        this.writer = notice.getWriter();
        this.createDate = notice.getCreateDate();
        this.url = notice.getUrl();
        this.contentText = notice.getContentText();
        this.attachments = attachments;
    }

    public static NoticeWithContentResponseDto of(Notice notice, List<AttachmentMeta> attachments) {
        return NoticeWithContentResponseDto.builder()
                .notice(notice)
                .attachments(attachments)
                .build();
    }
}
