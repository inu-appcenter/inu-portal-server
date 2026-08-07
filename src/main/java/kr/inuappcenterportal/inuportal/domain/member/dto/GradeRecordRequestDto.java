package kr.inuappcenterportal.inuportal.domain.member.dto;

public record GradeRecordRequestDto(
        String courseCode,
        String title,
        Integer credit,
        String grade,
        String isuName,
        String isuFldName,
        String note
) {
}
