package kr.inuappcenterportal.inuportal.domain.directory.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectoryCategory;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "directory_entry",
        indexes = {
                @Index(name = "idx_directory_entry_category", columnList = "category"),
                @Index(name = "idx_directory_entry_name", columnList = "person_name"),
                @Index(name = "idx_directory_entry_email", columnList = "email"),
                @Index(name = "idx_directory_entry_phone_normalized", columnList = "phone_number_normalized"),
                @Index(name = "idx_directory_entry_display_order", columnList = "display_order")
        }
)
public class DirectoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DirectoryCategory category;

    @Column(nullable = false)
    private String affiliation;

    @Column(name = "detail_affiliation", nullable = false)
    private String detailAffiliation;

    @Column(name = "person_name")
    private String name;

    @Column(nullable = false)
    private String position;

    @Column(length = 4000)
    private String duties;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "phone_number_normalized", length = 32)
    private String phoneNumberNormalized;

    @Column(length = 255)
    private String email;

    @Column(name = "profile_url", length = 1024)
    private String profileUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    @Builder
    private DirectoryEntry(DirectoryCategory category, String affiliation, String detailAffiliation, String name,
                           String position, String duties, String phoneNumber, String phoneNumberNormalized,
                           String email, String profileUrl, Integer displayOrder, LocalDateTime lastSyncedAt) {
        this.category = category;
        this.affiliation = affiliation;
        this.detailAffiliation = detailAffiliation;
        this.name = name;
        this.position = position;
        this.duties = duties;
        this.phoneNumber = phoneNumber;
        this.phoneNumberNormalized = phoneNumberNormalized;
        this.email = email;
        this.profileUrl = profileUrl;
        this.displayOrder = displayOrder;
        this.lastSyncedAt = lastSyncedAt;
    }

    public static DirectoryEntry create(DirectoryCategory category, String affiliation, String detailAffiliation,
                                        String position, String duties, String phoneNumber, String phoneNumberNormalized,
                                        Integer displayOrder, LocalDateTime lastSyncedAt) {
        return create(
                category,
                affiliation,
                detailAffiliation,
                null,
                position,
                duties,
                phoneNumber,
                phoneNumberNormalized,
                null,
                null,
                displayOrder,
                lastSyncedAt
        );
    }

    public static DirectoryEntry create(DirectoryCategory category, String affiliation, String detailAffiliation,
                                        String name, String position, String duties, String phoneNumber,
                                        String phoneNumberNormalized, String email, String profileUrl,
                                        Integer displayOrder, LocalDateTime lastSyncedAt) {
        return DirectoryEntry.builder()
                .category(category)
                .affiliation(affiliation)
                .detailAffiliation(detailAffiliation)
                .name(name)
                .position(position)
                .duties(duties)
                .phoneNumber(phoneNumber)
                .phoneNumberNormalized(phoneNumberNormalized)
                .email(email)
                .profileUrl(profileUrl)
                .displayOrder(displayOrder)
                .lastSyncedAt(lastSyncedAt)
                .build();
    }

    public DirectoryEntry withDisplayOrder(int displayOrder) {
        return DirectoryEntry.builder()
                .category(category)
                .affiliation(affiliation)
                .detailAffiliation(detailAffiliation)
                .name(name)
                .position(position)
                .duties(duties)
                .phoneNumber(phoneNumber)
                .phoneNumberNormalized(phoneNumberNormalized)
                .email(email)
                .profileUrl(profileUrl)
                .displayOrder(displayOrder)
                .lastSyncedAt(lastSyncedAt)
                .build();
    }
}
