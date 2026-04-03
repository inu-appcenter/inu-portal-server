package kr.inuappcenterportal.inuportal.domain.staff.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum StaffDirectoryCategory {

    HEADQUARTERS(1, "\uB300\uD559\uBCF8\uBD80", true),
    UNIVERSITY(2, "\uB300\uD559", false),
    GRADUATE_SCHOOL(3, "\uB300\uD559\uC6D0", false),
    AFFILIATED_INSTITUTION(4, "\uBD80\uC18D\uAE30\uAD00", true);

    private final int deptType;
    private final String label;
    private final boolean crawlable;

    public static List<StaffDirectoryCategory> crawlableCategories() {
        return Arrays.stream(values())
                .filter(StaffDirectoryCategory::isCrawlable)
                .toList();
    }
}
