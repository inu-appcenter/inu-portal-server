package kr.inuappcenterportal.inuportal.domain.councilNotice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.councilNotice.model.CouncilNotice;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
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
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createDate;

    @Schema(description = "수정일",example = "yyyy.mm.dd")
    @DateTimeFormat(pattern = "yyyy.MM.dd")
    private LocalDate modifiedDate;

    @Schema(description = "조회수")
    private Long view;

    @Schema(description = "이미지 수")
    private Long imageCount;

    @Builder
    private CouncilNoticeListDto (Long id, String title, LocalDate createDate, Long view, Long imageCount, LocalDate modifiedDate) {
        this.id = id;
        this.title = title;
        this.createDate = createDate;
        this.modifiedDate = modifiedDate;
        this.view = view;
        this.imageCount = imageCount;
    }

    public static CouncilNoticeListDto of(CouncilNotice councilNotice){
        return CouncilNoticeListDto.builder()
                .id(councilNotice.getId())
                .title(councilNotice.getTitle())
                .createDate(councilNotice.getCreateDate())
                .modifiedDate(councilNotice.getModifiedDate())
                .view(councilNotice.getView())
                .imageCount(councilNotice.getImageCount())
                .build();
    }
}
