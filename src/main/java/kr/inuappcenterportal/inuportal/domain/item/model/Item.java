package kr.inuappcenterportal.inuportal.domain.item.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.item.dto.ItemRegister;
import kr.inuappcenterportal.inuportal.domain.item.enums.ItemCategory;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(value = EnumType.STRING)
    private ItemCategory itemCategory;

    private String name;

    private int totalQuantity;

    private int deposit;

    @Builder
    public Item(ItemCategory itemCategory, String name, int totalQuantity, int deposit) {
        this.itemCategory = itemCategory;
        this.name = name;
        this.totalQuantity = totalQuantity;
        this.deposit = deposit;
    }

    public static Item create(ItemRegister itemRegister) {
        return Item.builder()
                .name(itemRegister.getName())
                .itemCategory(ItemCategory.from(itemRegister.getItemCategory()))
                .name(itemRegister.getName())
                .totalQuantity(itemRegister.getTotalQuantity())
                .deposit(itemRegister.getDeposit())
                .build();
    }

    public void update(ItemCategory itemCategory, String name, int totalQuantity, int deposit) {
        this.itemCategory = itemCategory;
        this.name = name;
        this.totalQuantity = totalQuantity;
        this.deposit = deposit;
    }

}
