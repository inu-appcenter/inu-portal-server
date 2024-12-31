package kr.inuappcenterportal.inuportal.domain.councilNotice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.councilNotice.model.CouncilNotice;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Schema(description = "총학생회 공지사항 리스트 응답 Dto")
@Getter
@NoArgsConstructor
public class CouncilNoticeListDto {
    @Schema(description = "총학생회 공지사항 데이터베이스 id 값")
    private Long id;

    @Schema(description = "제목",example = "제목")
    private String title;

    @Schema(description = "작성일",example = "작성일")
    private String createDate;

    @Schema(description = "조회수")
    private Long view;

    @Builder
    private CouncilNoticeListDto (Long id, String title, String createDate, Long view){
        this.id = id;
        this.title = title;
        this.createDate = createDate;
        this.view = view;
    }

    public static CouncilNoticeListDto of(CouncilNotice councilNotice){
        return CouncilNoticeListDto.builder()
                .id(councilNotice.getId())
                .title(councilNotice.getTitle())
                .createDate(councilNotice.getCreateDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .view(councilNotice.getView())
                .build();
    }
}
