package kr.inuappcenterportal.inuportal.domain.item.dto;

import kr.inuappcenterportal.inuportal.domain.item.model.Item;
import lombok.Builder;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ItemDetail {

    private String itemCategory;
    private String name;
    private int totalQuantity;
    private int deposit;
    @DateTimeFormat(pattern = "yyyy.MM.dd")
    private LocalDate createDate;
    @DateTimeFormat(pattern = "yyyy.MM.dd HH:mm:ss")
    private LocalDateTime modifiedDate;

    public static ItemDetail from(Item item) {
        return ItemDetail.builder()
                .itemCategory(item.getItemCategory().name())
                .name(item.getName())
                .totalQuantity(item.getTotalQuantity())
                .deposit(item.getDeposit())
                .createDate(item.getCreateDate())
                .modifiedDate(item.getModifiedDate())
                .build();
    }
}
