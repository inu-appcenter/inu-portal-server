package kr.inuappcenterportal.inuportal.domain.directory.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum DirectoryCategory {

    HEADQUARTERS(1, "대학본부", true),
    UNIVERSITY(2, "대학", false),
    GRADUATE_SCHOOL(3, "대학원", false),
    AFFILIATED_INSTITUTION(4, "부속기관", true);

    private final int deptType;
    private final String label;
    private final boolean crawlable;

    public static List<DirectoryCategory> crawlableCategories() {
        return Arrays.stream(values())
                .filter(DirectoryCategory::isCrawlable)
                .toList();
    }

    public static List<DirectoryCategory> inventoryCategories() {
        return List.of(UNIVERSITY, GRADUATE_SCHOOL);
    }
}
