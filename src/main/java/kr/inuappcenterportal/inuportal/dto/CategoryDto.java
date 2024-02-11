package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "카테고리 추가 Dto")
@Getter
@NoArgsConstructor
public class CategoryDto {
    @Schema(description = "카테고리",example = "수강신청")
    @NotBlank
    private String category;

    @Builder
    public CategoryDto(String category){
        this.category = category;
    }
}
