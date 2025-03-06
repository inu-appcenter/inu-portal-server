package kr.inuappcenterportal.inuportal.domain.item.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.item.dto.ItemRegister;
import kr.inuappcenterportal.inuportal.domain.item.enums.ItemCategory;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "item")
public class Item{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_category")
    @Enumerated(value = EnumType.STRING)
    private ItemCategory itemCategory;

    private String name;

    @Column(name = "total_quantity")
    private int totalQuantity;

    private int deposit;

    @Column(name = "image_count")
    private int imageCount;

    @Column(name = "create_date")
    private LocalDate createDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @Builder
    public Item(ItemCategory itemCategory, String name, int totalQuantity, int deposit, int imageCount) {
        this.itemCategory = itemCategory;
        this.name = name;
        this.totalQuantity = totalQuantity;
        this.deposit = deposit;
        this.imageCount = imageCount;
        this.createDate = LocalDate.now();
        this.modifiedDate = LocalDateTime.now();
    }

    public static Item create(ItemRegister itemRegister, int imageCount) {
        return Item.builder()
                .name(itemRegister.getName())
                .itemCategory(ItemCategory.from(itemRegister.getItemCategory()))
                .name(itemRegister.getName())
                .totalQuantity(itemRegister.getTotalQuantity())
                .deposit(itemRegister.getDeposit())
                .imageCount(imageCount)
                .build();
    }

    public void update(ItemCategory itemCategory, String name, int totalQuantity, int deposit, int imageCount) {
        this.itemCategory = itemCategory;
        this.name = name;
        this.totalQuantity = totalQuantity;
        this.deposit = deposit;
        this.imageCount = imageCount;
        this.modifiedDate = LocalDateTime.now();
    }

}
