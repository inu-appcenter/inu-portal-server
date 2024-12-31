package kr.inuappcenterportal.inuportal.domain.councilNotice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.councilNotice.model.CouncilNotice;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Schema(description = "총학생회 공지사항 응답Dto")
@Getter
@NoArgsConstructor
public class CouncilNoticeResponseDto {
    @Schema(description = "총학생회 공지사항 데이터베이스 아이디값")
    private Long id;

    @Schema(description = "제목",example = "제목")
    private String title;

    @Schema(description = "내용", example = "내용")
    private String content;

    @Schema(description = "조회수")
    private Long view;

    @Schema(description = "생성일",example = "yyyy-mm-dd")
    private String createDate;

    @Schema(description = "수정일",example = "yyyy-mm-dd")
    private String modifiedDate;

    @Schema(description = "이미지 갯수")
    private Long imageCount;

    @Builder
    private CouncilNoticeResponseDto(Long id, String title, String content, Long view, String createDate, String modifiedDate, Long imageCount){
        this.id = id;
        this.title = title;
        this.content = content;
        this.view = view;
        this.createDate = createDate;
        this.modifiedDate = modifiedDate;
        this.imageCount = imageCount;
    }

    public static CouncilNoticeResponseDto of(CouncilNotice councilNotice){
        return CouncilNoticeResponseDto.builder()
                .id(councilNotice.getId())
                .title(councilNotice.getTitle())
                .content(councilNotice.getContent())
                .view(councilNotice.getView())
                .createDate(councilNotice.getCreateDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .modifiedDate(councilNotice.getModifiedDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .imageCount(councilNotice.getImageCount())
                .build();
    }
}
