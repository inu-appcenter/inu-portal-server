package kr.inuappcenterportal.inuportal.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.post.dto.PostListResponseDto;
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

    @Schema(description = "총 게시글 수")
    private Long total;

    @Schema(description = "게시글 리스트")
    private List<PostListResponseDto> posts;

    @Builder
    private ListResponseDto(long pages, long total, List<PostListResponseDto> posts){
        this.pages = pages;
        this.posts = posts;
        this.total = total;
    }

    public static ListResponseDto of(long pages, long total, List<PostListResponseDto> posts){
        return ListResponseDto.builder()
                .pages(pages)
                .posts(posts)
                .total(total)
                .build();
    }
}
