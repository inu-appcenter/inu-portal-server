package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "게시글, 페이지수 리스트 응답Dto")
@Getter
@NoArgsConstructor
public class ListResponseDto {

    @Schema(description = "총 페이지 수")
    private Long pages;

    @Schema(description = "게시글 리스트")
    private List<PostListResponseDto> posts;

    @Builder
    private ListResponseDto(long pages, List<PostListResponseDto> posts){
        this.pages = pages;
        this.posts = posts;
    }

    public static ListResponseDto of(long pages, List<PostListResponseDto> posts){
        return ListResponseDto.builder()
                .pages(pages)
                .posts(posts)
                .build();
    }
}
