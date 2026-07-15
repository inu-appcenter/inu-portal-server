package kr.inuappcenterportal.inuportal.domain.timeTable.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Visibility {
    PUBLIC("전체공개"),
    PROTECTED("일부공개"),
    PRIVATE("비공개");

    private final String description;
}
