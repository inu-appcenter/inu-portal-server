package kr.inuappcenterportal.inuportal.domain.item.dto;

import kr.inuappcenterportal.inuportal.domain.item.enums.ItemCategory;
import kr.inuappcenterportal.inuportal.domain.item.model.Item;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ItemPreview {

    private String itemCategory;
    private String name;
    private int totalQuantity;
    private int deposit;

    public static ItemPreview from(Item item) {
        return ItemPreview.builder()
                .itemCategory(item.getItemCategory().name())
                .name(item.getName())
                .totalQuantity(item.getTotalQuantity())
                .deposit(item.getDeposit())
                .build();
    }
}
