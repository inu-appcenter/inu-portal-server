package kr.inuappcenterportal.inuportal.domain.directory.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class DirectorySourceSyncResponse {

    private final LocalDateTime syncedAt;
    private final long totalCount;
    private final List<DirectoryCategoryCountResponse> categories;

    @Builder
    private DirectorySourceSyncResponse(LocalDateTime syncedAt, long totalCount,
                                        List<DirectoryCategoryCountResponse> categories) {
        this.syncedAt = syncedAt;
        this.totalCount = totalCount;
        this.categories = categories;
    }

    public static DirectorySourceSyncResponse of(LocalDateTime syncedAt, long totalCount,
                                                 List<DirectoryCategoryCountResponse> categories) {
        return DirectorySourceSyncResponse.builder()
                .syncedAt(syncedAt)
                .totalCount(totalCount)
                .categories(categories)
                .build();
    }
}
