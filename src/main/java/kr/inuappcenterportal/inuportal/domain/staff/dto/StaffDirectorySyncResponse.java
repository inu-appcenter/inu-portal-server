package kr.inuappcenterportal.inuportal.domain.staff.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class StaffDirectorySyncResponse {

    private final LocalDateTime syncedAt;
    private final long totalCount;
    private final List<StaffDirectoryCategoryCountResponse> categories;

    @Builder
    private StaffDirectorySyncResponse(LocalDateTime syncedAt, long totalCount, List<StaffDirectoryCategoryCountResponse> categories) {
        this.syncedAt = syncedAt;
        this.totalCount = totalCount;
        this.categories = categories;
    }

    public static StaffDirectorySyncResponse of(LocalDateTime syncedAt, long totalCount,
                                                List<StaffDirectoryCategoryCountResponse> categories) {
        return StaffDirectorySyncResponse.builder()
                .syncedAt(syncedAt)
                .totalCount(totalCount)
                .categories(categories)
                .build();
    }
}
