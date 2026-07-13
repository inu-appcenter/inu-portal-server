package kr.inuappcenterportal.inuportal.domain.course.dto;

import kr.inuappcenterportal.inuportal.domain.course.enums.CourseOfferingGuideType;

public record DownloadedGuideFileDto(
        String fileName,
        String downloadUrl,
        CourseOfferingGuideType type,
        byte[] content
) {
}
