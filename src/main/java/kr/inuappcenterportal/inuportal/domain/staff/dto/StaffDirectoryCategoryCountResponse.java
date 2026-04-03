package kr.inuappcenterportal.inuportal.domain.staff.dto;

import kr.inuappcenterportal.inuportal.domain.staff.enums.StaffDirectoryCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
public class StaffDirectoryCategoryCountResponse {

    private final StaffDirectoryCategory category;
    private final String categoryName;
    private final long count;

    @Builder
    private StaffDirectoryCategoryCountResponse(StaffDirectoryCategory category, String categoryName, long count) {
        this.category = category;
        this.categoryName = categoryName;
        this.count = count;
    }

    public static StaffDirectoryCategoryCountResponse of(StaffDirectoryCategory category, long count) {
        return StaffDirectoryCategoryCountResponse.builder()
                .category(category)
                .categoryName(category.getLabel())
                .count(count)
                .build();
    }
}
