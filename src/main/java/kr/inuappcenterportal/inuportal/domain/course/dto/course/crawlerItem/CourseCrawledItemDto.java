package kr.inuappcenterportal.inuportal.domain.course.dto.course.crawlerItem;

public record CourseCrawledItemDto(
        String title,
        String content,
        String targetGrade,
        String targetTerm,
        String completionDivision,
        Integer credit
) {
}