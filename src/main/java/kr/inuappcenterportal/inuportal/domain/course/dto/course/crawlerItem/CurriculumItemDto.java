package kr.inuappcenterportal.inuportal.domain.course.dto.course.crawlerItem;

public record CurriculumItemDto(
        String targetGrade,
        String targetTerm,
        String completionDivision,
        String title,
        Integer credit
) {
}
