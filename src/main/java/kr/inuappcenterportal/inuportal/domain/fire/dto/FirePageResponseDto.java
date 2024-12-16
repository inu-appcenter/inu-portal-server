package kr.inuappcenterportal.inuportal.domain.fire.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


@Schema(description = "AI 횃불이 정보, 페이지수 리스트 응답Dto")
@Getter
@NoArgsConstructor
public class FirePageResponseDto {
    @Schema(description = "총 페이지 수")
    private Long pages;

    @Schema(description = "총 생성된 횃불이 수")
    private Long total;

    @Schema(description = "횃불이 리스트")
    private List<FireListResponseDto> fires;

    @Builder
    private FirePageResponseDto(long pages, long total, List<FireListResponseDto> fires){
        this.pages = pages;
        this.total = total;
        this.fires = fires;
    }

    public static FirePageResponseDto of(long pages, long total, List<FireListResponseDto> fires){
        return FirePageResponseDto.builder()
                .pages(pages)
                .total(total)
                .fires(fires)
                .build();
    }
}
