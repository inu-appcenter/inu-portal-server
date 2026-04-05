package kr.inuappcenterportal.inuportal.domain.directory.dto;

import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectoryCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
public class DirectoryCategoryCountResponse {

    private final DirectoryCategory category;
    private final String categoryName;
    private final long count;

    @Builder
    private DirectoryCategoryCountResponse(DirectoryCategory category, String categoryName, long count) {
        this.category = category;
        this.categoryName = categoryName;
        this.count = count;
    }

    public static DirectoryCategoryCountResponse of(DirectoryCategory category, long count) {
        return DirectoryCategoryCountResponse.builder()
                .category(category)
                .categoryName(category.getLabel())
                .count(count)
                .build();
    }
}
