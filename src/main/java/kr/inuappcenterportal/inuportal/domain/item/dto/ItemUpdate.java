package kr.inuappcenterportal.inuportal.domain.item.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ItemUpdate {

    @NotBlank
    private String itemCategory;
    @NotBlank
    private String name;
    @Min(value = 1)
    private int totalQuantity;
    @Min(value = 10)
    private int deposit;
}
