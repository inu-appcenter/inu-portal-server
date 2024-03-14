package kr.inuappcenterportal.inuportal.dto;

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

    @Schema(description = "공지사항 리스트")
    private List<NoticeListResponseDto> notices;

    @Builder
    private NoticePageResponseDto(long pages, List<NoticeListResponseDto> notices){
        this.pages = pages;
        this.notices = notices;
    }

    public static NoticePageResponseDto of(long pages, List<NoticeListResponseDto> notices){
        return NoticePageResponseDto.builder()
                .pages(pages)
                .notices(notices)
                .build();
    }
}
