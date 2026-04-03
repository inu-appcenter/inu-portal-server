package kr.inuappcenterportal.inuportal.domain.directory.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "college_office_contact",
        indexes = {
                @Index(name = "idx_college_office_contact_college_name", columnList = "college_name"),
                @Index(name = "idx_college_office_contact_department_name", columnList = "department_name"),
                @Index(name = "idx_college_office_contact_phone_normalized", columnList = "office_phone_number_normalized"),
                @Index(name = "idx_college_office_contact_display_order", columnList = "display_order")
        }
)
public class CollegeOfficeContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "college_name", nullable = false)
    private String collegeName;

    @Column(name = "college_location_summary")
    private String collegeLocationSummary;

    @Column(name = "department_name", nullable = false)
    private String departmentName;

    @Column(name = "office_phone_number")
    private String officePhoneNumber;

    @Column(name = "office_phone_number_normalized", length = 32)
    private String officePhoneNumberNormalized;

    @Column(name = "homepage_url", length = 1024)
    private String homepageUrl;

    @Column(name = "office_location")
    private String officeLocation;

    @Column(name = "source_url", nullable = false, length = 1024)
    private String sourceUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    @Builder
    private CollegeOfficeContact(String collegeName, String collegeLocationSummary, String departmentName,
                                 String officePhoneNumber, String officePhoneNumberNormalized, String homepageUrl,
                                 String officeLocation, String sourceUrl, Integer displayOrder,
                                 LocalDateTime lastSyncedAt) {
        this.collegeName = collegeName;
        this.collegeLocationSummary = collegeLocationSummary;
        this.departmentName = departmentName;
        this.officePhoneNumber = officePhoneNumber;
        this.officePhoneNumberNormalized = officePhoneNumberNormalized;
        this.homepageUrl = homepageUrl;
        this.officeLocation = officeLocation;
        this.sourceUrl = sourceUrl;
        this.displayOrder = displayOrder;
        this.lastSyncedAt = lastSyncedAt;
    }

    public static CollegeOfficeContact create(String collegeName, String collegeLocationSummary, String departmentName,
                                              String officePhoneNumber, String officePhoneNumberNormalized,
                                              String homepageUrl, String officeLocation, String sourceUrl,
                                              Integer displayOrder, LocalDateTime lastSyncedAt) {
        return CollegeOfficeContact.builder()
                .collegeName(collegeName)
                .collegeLocationSummary(collegeLocationSummary)
                .departmentName(departmentName)
                .officePhoneNumber(officePhoneNumber)
                .officePhoneNumberNormalized(officePhoneNumberNormalized)
                .homepageUrl(homepageUrl)
                .officeLocation(officeLocation)
                .sourceUrl(sourceUrl)
                .displayOrder(displayOrder)
                .lastSyncedAt(lastSyncedAt)
                .build();
    }
}
