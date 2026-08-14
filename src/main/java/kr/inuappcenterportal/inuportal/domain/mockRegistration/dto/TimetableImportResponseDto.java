package kr.inuappcenterportal.inuportal.domain.mockRegistration.dto;

import java.util.List;

public record TimetableImportResponseDto(
        Long timetableId,
        String timetableName,
        int addedCount,
        int skippedCount,
        List<Long> addedCourseOfferingIds,
        List<SkippedItem> skippedItems
) {
    public record SkippedItem(Long courseOfferingId, String reason) {}
}
