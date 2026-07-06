package kr.inuappcenterportal.inuportal.domain.course.dto;

public record CourseCrawledItemDto(
        String title,
        String content,
        String targetGrade,
        String targetTerm,
        String completionDivision,
        String credit
) {
}
