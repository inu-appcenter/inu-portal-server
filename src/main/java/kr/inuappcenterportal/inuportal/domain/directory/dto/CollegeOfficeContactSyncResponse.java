package kr.inuappcenterportal.inuportal.domain.directory.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CollegeOfficeContactSyncResponse {

    private final LocalDateTime syncedAt;
    private final long totalCount;

    @Builder
    private CollegeOfficeContactSyncResponse(LocalDateTime syncedAt, long totalCount) {
        this.syncedAt = syncedAt;
        this.totalCount = totalCount;
    }

    public static CollegeOfficeContactSyncResponse of(LocalDateTime syncedAt, long totalCount) {
        return CollegeOfficeContactSyncResponse.builder()
                .syncedAt(syncedAt)
                .totalCount(totalCount)
                .build();
    }
}
