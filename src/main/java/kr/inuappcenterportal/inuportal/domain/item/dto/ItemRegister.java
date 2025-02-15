package kr.inuappcenterportal.inuportal.domain.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "물품 등록 Dto")
public class ItemRegister {

    @NotBlank
    @Schema(description = "카테고리",example = "카테고리")
    private String itemCategory;
    @NotBlank
    @Schema(description = "이름",example = "이름")
    private String name;
    @Min(value = 0)
    @Schema(description = "총 개수",example = "2")
    private int totalQuantity;
    @Min(value = 0)
    @Schema(description = "예치금",example = "2000")
    private int deposit;

}
