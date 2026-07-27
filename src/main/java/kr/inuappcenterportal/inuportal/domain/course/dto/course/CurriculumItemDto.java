package kr.inuappcenterportal.inuportal.domain.course.dto.course;

public record CurriculumItemDto(
        String targetGrade,
        String targetTerm,
        String completionDivision,
        String title,
        String credit
) {
}
