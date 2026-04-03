package kr.inuappcenterportal.inuportal.domain.directory.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class DirectorySyncResponse {

    private final LocalDateTime syncedAt;
    private final long totalCount;
    private final List<DirectoryCategoryCountResponse> categories;

    @Builder
    private DirectorySyncResponse(LocalDateTime syncedAt, long totalCount, List<DirectoryCategoryCountResponse> categories) {
        this.syncedAt = syncedAt;
        this.totalCount = totalCount;
        this.categories = categories;
    }

    public static DirectorySyncResponse of(LocalDateTime syncedAt, long totalCount,
                                           List<DirectoryCategoryCountResponse> categories) {
        return DirectorySyncResponse.builder()
                .syncedAt(syncedAt)
                .totalCount(totalCount)
                .categories(categories)
                .build();
    }
}
