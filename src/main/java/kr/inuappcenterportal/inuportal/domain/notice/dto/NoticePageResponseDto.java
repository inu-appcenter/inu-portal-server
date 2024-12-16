package kr.inuappcenterportal.inuportal.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "공시사항 리스트,페이지 응답Dto")
public class NoticePageResponseDto {
    @Schema(description = "총 페이지 수")
    private Long pages;

    @Schema(description = "총 공지 수")
    private Long total;

    @Schema(description = "공지사항 리스트")
    private List<NoticeListResponseDto> notices;

    @Builder
    private NoticePageResponseDto(long pages, List<NoticeListResponseDto> notices, long total){
        this.pages = pages;
        this.notices = notices;
        this.total = total;
    }

    public static NoticePageResponseDto of(long pages,long total ,List<NoticeListResponseDto> notices){
        return NoticePageResponseDto.builder()
                .pages(pages)
                .notices(notices)
                .total(total)
                .build();
    }
}
