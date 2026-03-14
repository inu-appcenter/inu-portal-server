package kr.inuappcenterportal.inuportal.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(description = "카테고리별 게시글 응답 DTO")
@Getter
public class CategoryPostResponseDto {
    @Schema(description = "카테고리명", example = "자유게시판")
    private String category;
    @Schema(description = "해당 카테고리의 게시글 리스트")
    private List<PostListResponseDto> posts;

    @Builder
    public CategoryPostResponseDto(String category, List<PostListResponseDto> posts) {
        this.category = category;
        this.posts = posts;
    }
}
