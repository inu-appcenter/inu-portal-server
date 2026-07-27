package kr.inuappcenterportal.inuportal.domain.timeTable.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TimeTableItemType {
    COURSE("강의"),
    CUSTOM("커스텀일정");

    private final String description;
}
