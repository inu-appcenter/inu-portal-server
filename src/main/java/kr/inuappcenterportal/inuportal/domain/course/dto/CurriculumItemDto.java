package kr.inuappcenterportal.inuportal.domain.course.dto;

public record CurriculumItemDto(
        String title,
        String credit,
        String targetGrade,
        String targetTerm,
        String completionDivision
) {
}
