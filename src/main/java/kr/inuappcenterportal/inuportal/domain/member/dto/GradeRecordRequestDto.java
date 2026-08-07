package kr.inuappcenterportal.inuportal.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GradeRecordRequestDto(
        String courseCode,
        @NotBlank String title,
        @NotNull Integer credit,
        String grade,
        @NotNull Boolean isMajor,
        String isCourseRepetition
) {
}
