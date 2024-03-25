package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.Notice;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "학교 공지사항 리스트 응답 Dto")
@Getter
@NoArgsConstructor
public class NoticeListResponseDto {

    @Schema(description = "공지사항 데이터베이스 id 값")
    private Long id;

    @Schema(description = "카테고리",example = "카테고리")
    private String category;

    @Schema(description = "제목",example = "제목")
    private String title;

    @Schema(description = "작성자",example = "작성자")
    private String writer;

    @Schema(description = "작성일",example = "작성일")
    private String createDate;
    @Schema(description = "조회수")
    private Long view;

    @Schema(description = "링크 url",example = "url")
    private String url;

    @Builder
    private NoticeListResponseDto(Notice notice){
        this.id =notice.getId();
        this.category = notice.getCategory();
        this.title = notice.getTitle();
        this.writer = notice.getWriter();
        this.createDate = notice.getCreateDate();
        this.view = notice.getView();
        this.url = notice.getUrl();
    }

    public static NoticeListResponseDto of(Notice notice){
        return  NoticeListResponseDto.builder().notice(notice).build();
    }
}
