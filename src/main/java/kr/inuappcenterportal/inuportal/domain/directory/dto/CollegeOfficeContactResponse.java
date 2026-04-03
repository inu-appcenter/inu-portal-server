package kr.inuappcenterportal.inuportal.domain.directory.dto;

import kr.inuappcenterportal.inuportal.domain.directory.model.CollegeOfficeContact;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CollegeOfficeContactResponse {

    private final Long id;
    private final String collegeName;
    private final String collegeLocationSummary;
    private final String departmentName;
    private final String officePhoneNumber;
    private final String homepageUrl;
    private final String officeLocation;
    private final String sourceUrl;
    private final LocalDateTime lastSyncedAt;

    @Builder
    private CollegeOfficeContactResponse(Long id, String collegeName, String collegeLocationSummary,
                                         String departmentName, String officePhoneNumber, String homepageUrl,
                                         String officeLocation, String sourceUrl, LocalDateTime lastSyncedAt) {
        this.id = id;
        this.collegeName = collegeName;
        this.collegeLocationSummary = collegeLocationSummary;
        this.departmentName = departmentName;
        this.officePhoneNumber = officePhoneNumber;
        this.homepageUrl = homepageUrl;
        this.officeLocation = officeLocation;
        this.sourceUrl = sourceUrl;
        this.lastSyncedAt = lastSyncedAt;
    }

    public static CollegeOfficeContactResponse of(CollegeOfficeContact contact) {
        return CollegeOfficeContactResponse.builder()
                .id(contact.getId())
                .collegeName(contact.getCollegeName())
                .collegeLocationSummary(contact.getCollegeLocationSummary())
                .departmentName(contact.getDepartmentName())
                .officePhoneNumber(contact.getOfficePhoneNumber())
                .homepageUrl(contact.getHomepageUrl())
                .officeLocation(contact.getOfficeLocation())
                .sourceUrl(contact.getSourceUrl())
                .lastSyncedAt(contact.getLastSyncedAt())
                .build();
    }
}
