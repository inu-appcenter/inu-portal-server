package kr.inuappcenterportal.inuportal.domain.course.dto.course;

public record CourseCrawledItemDto(
        String title,
        String content,
        String targetGrade,
        String targetTerm,
        String completionDivision,
        String credit
) {
}