package kr.inuappcenterportal.inuportal.domain.item.dto;

import lombok.Getter;

@Getter
public class ItemUpdate {

    private String itemCategory;
    private String name;
    private int totalQuantity;
    private int deposit;
}
