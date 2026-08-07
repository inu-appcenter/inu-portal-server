package kr.inuappcenterportal.inuportal.domain.member.dto;

public record GradeRecordUpdateRequestDto(
        Integer credit,
        String grade,
        Boolean isMajor,
        String isCourseRepetition
) {
}
