package kr.inuappcenterportal.inuportal.domain.staff.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.staff.enums.StaffDirectoryCategory;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "staff_directory_entry",
        indexes = {
                @Index(name = "idx_staff_directory_category", columnList = "category"),
                @Index(name = "idx_staff_directory_phone_normalized", columnList = "phone_number_normalized"),
                @Index(name = "idx_staff_directory_display_order", columnList = "display_order")
        }
)
public class StaffDirectoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StaffDirectoryCategory category;

    @Column(nullable = false)
    private String affiliation;

    @Column(name = "detail_affiliation", nullable = false)
    private String detailAffiliation;

    @Column(nullable = false)
    private String position;

    @Column(length = 4000)
    private String duties;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "phone_number_normalized", length = 32)
    private String phoneNumberNormalized;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    @Builder
    private StaffDirectoryEntry(StaffDirectoryCategory category, String affiliation, String detailAffiliation,
                                String position, String duties, String phoneNumber,
                                String phoneNumberNormalized, Integer displayOrder,
                                LocalDateTime lastSyncedAt) {
        this.category = category;
        this.affiliation = affiliation;
        this.detailAffiliation = detailAffiliation;
        this.position = position;
        this.duties = duties;
        this.phoneNumber = phoneNumber;
        this.phoneNumberNormalized = phoneNumberNormalized;
        this.displayOrder = displayOrder;
        this.lastSyncedAt = lastSyncedAt;
    }

    public static StaffDirectoryEntry create(StaffDirectoryCategory category, String affiliation, String detailAffiliation,
                                             String position, String duties, String phoneNumber,
                                             String phoneNumberNormalized, Integer displayOrder,
                                             LocalDateTime lastSyncedAt) {
        return StaffDirectoryEntry.builder()
                .category(category)
                .affiliation(affiliation)
                .detailAffiliation(detailAffiliation)
                .position(position)
                .duties(duties)
                .phoneNumber(phoneNumber)
                .phoneNumberNormalized(phoneNumberNormalized)
                .displayOrder(displayOrder)
                .lastSyncedAt(lastSyncedAt)
                .build();
    }
}
