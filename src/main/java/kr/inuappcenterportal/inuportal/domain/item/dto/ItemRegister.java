package kr.inuappcenterportal.inuportal.domain.item.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ItemRegister {

    @NotBlank
    private String itemCategory;
    @NotBlank
    private String name;
    @Min(value = 0)
    private int totalQuantity;
    @Min(value = 0)
    private int deposit;

}
