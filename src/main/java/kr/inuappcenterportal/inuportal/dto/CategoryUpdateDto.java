package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "카테고리명 변경 Dto")
@Getter
@NoArgsConstructor
public class CategoryUpdateDto {
    @Schema(description = "변경할 카테고리 이름",example = "수강신청")
    private String category;
    @Schema(description = "새로운 카테고리 이름",example = "기숙사")
    private String newCategory;

    @Builder
    public CategoryUpdateDto(String category, String newCategory){
        this.category =category;
        this.newCategory = newCategory;
    }
}
