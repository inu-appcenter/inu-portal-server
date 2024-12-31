package kr.inuappcenterportal.inuportal.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "게시글, 페이지수 리스트 응답Dto")
@Getter
@NoArgsConstructor
public class ListResponseDto<T>{

    @Schema(description = "총 페이지 수")
    private Long pages;

    @Schema(description = "총 게시글 수")
    private Long total;

    @Schema(description = "게시글 리스트")
    private List<T> contents;

    @Builder
    private ListResponseDto(long pages, long total, List<T> contents){
        this.pages = pages;
        this.contents = contents;
        this.total = total;
    }

    public static <T>ListResponseDto<T> of(long pages, long total, List<T> contents){
        return ListResponseDto.<T>builder()
                .pages(pages)
                .contents(contents)
                .total(total)
                .build();
    }
}
