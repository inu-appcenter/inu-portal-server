package kr.inuappcenterportal.inuportal.domain.staff.dto;

import kr.inuappcenterportal.inuportal.domain.staff.enums.StaffDirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.staff.model.StaffDirectoryEntry;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class StaffDirectoryEntryResponse {

    private final Long id;
    private final StaffDirectoryCategory category;
    private final String categoryName;
    private final String affiliation;
    private final String detailAffiliation;
    private final String position;
    private final String duties;
    private final String phoneNumber;
    private final LocalDateTime lastSyncedAt;

    @Builder
    private StaffDirectoryEntryResponse(Long id, StaffDirectoryCategory category, String categoryName,
                                        String affiliation, String detailAffiliation, String position,
                                        String duties, String phoneNumber, LocalDateTime lastSyncedAt) {
        this.id = id;
        this.category = category;
        this.categoryName = categoryName;
        this.affiliation = affiliation;
        this.detailAffiliation = detailAffiliation;
        this.position = position;
        this.duties = duties;
        this.phoneNumber = phoneNumber;
        this.lastSyncedAt = lastSyncedAt;
    }

    public static StaffDirectoryEntryResponse of(StaffDirectoryEntry entry) {
        return StaffDirectoryEntryResponse.builder()
                .id(entry.getId())
                .category(entry.getCategory())
                .categoryName(entry.getCategory().getLabel())
                .affiliation(entry.getAffiliation())
                .detailAffiliation(entry.getDetailAffiliation())
                .position(entry.getPosition())
                .duties(entry.getDuties())
                .phoneNumber(entry.getPhoneNumber())
                .lastSyncedAt(entry.getLastSyncedAt())
                .build();
    }
}
