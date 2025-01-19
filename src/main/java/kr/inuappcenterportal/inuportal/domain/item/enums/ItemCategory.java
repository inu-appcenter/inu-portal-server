package kr.inuappcenterportal.inuportal.domain.item.enums;

import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;

public enum ItemCategory {
    BROADCAST_EQUIPMENT, TENT, SPORTS_EQUIPMENT, OTHER;

    public static ItemCategory from(String name) {
        try {
            return ItemCategory.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new MyException(MyErrorCode.INVALID_CATEGORY);
        }
    }
}
