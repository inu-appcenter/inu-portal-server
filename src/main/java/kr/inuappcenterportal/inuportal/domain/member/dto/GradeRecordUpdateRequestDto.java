package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record GradeRecordUpdateRequestDto(
        @NotNull Integer credit,
        @Schema(
                description = "성적. 성적 미발표 시 null",
                allowableValues = {"A+", "A0", "B+", "B0", "C+", "C0", "D+", "D0", "F", "P", "NP"},
                example = "A+",
                nullable = true
        )
        String grade,
        @NotNull Boolean isMajor,
        @Schema(
                description = "재수강 성적 취소 여부",
                example = "true"
        )
        @NotNull Boolean isCourseRepetition
) {
}
