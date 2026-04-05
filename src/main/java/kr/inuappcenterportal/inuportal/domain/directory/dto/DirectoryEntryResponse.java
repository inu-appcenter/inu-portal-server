package kr.inuappcenterportal.inuportal.domain.directory.dto;

import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectoryEntry;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DirectoryEntryResponse {

    private final Long id;
    private final DirectoryCategory category;
    private final String categoryName;
    private final String affiliation;
    private final String detailAffiliation;
    private final String name;
    private final String position;
    private final String duties;
    private final String phoneNumber;
    private final String email;
    private final String profileUrl;
    private final LocalDateTime lastSyncedAt;

    @Builder
    private DirectoryEntryResponse(Long id, DirectoryCategory category, String categoryName,
                                   String affiliation, String detailAffiliation, String name, String position,
                                   String duties, String phoneNumber, String email, String profileUrl,
                                   LocalDateTime lastSyncedAt) {
        this.id = id;
        this.category = category;
        this.categoryName = categoryName;
        this.affiliation = affiliation;
        this.detailAffiliation = detailAffiliation;
        this.name = name;
        this.position = position;
        this.duties = duties;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.profileUrl = profileUrl;
        this.lastSyncedAt = lastSyncedAt;
    }

    public static DirectoryEntryResponse of(DirectoryEntry entry) {
        return DirectoryEntryResponse.builder()
                .id(entry.getId())
                .category(entry.getCategory())
                .categoryName(entry.getCategory().getLabel())
                .affiliation(entry.getAffiliation())
                .detailAffiliation(entry.getDetailAffiliation())
                .name(entry.getName())
                .position(entry.getPosition())
                .duties(entry.getDuties())
                .phoneNumber(entry.getPhoneNumber())
                .email(entry.getEmail())
                .profileUrl(entry.getProfileUrl())
                .lastSyncedAt(entry.getLastSyncedAt())
                .build();
    }
}
