package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GradeRecordRequestDto(
        String courseCode,
        @NotBlank String title,
        @NotNull Integer credit,
        @Schema(
                description = "성적. 성적 미발표 시 null",
                allowableValues = {"A+", "A0", "B+", "B0", "C+", "C0", "D+", "D0", "F", "P", "NP"},
                example = "B+",
                nullable = true
        )
        String grade,
        @NotNull Boolean isMajor,
        @Schema(
                description = "재수강 성적 취소 여부",
                example = "false"
        )
        @NotNull Boolean isCourseRepetition,
        @Schema(
                description = "이수구분(전공기초 / 전공핵심 / 심화교양 …) 원문. 없으면 null",
                example = "전공핵심",
                nullable = true
        )
        @Size(max = 255) String isuName,
        @Schema(
                description = "이수영역(전공심화 / 사회 …) 원문. 없으면 null",
                example = "전공심화",
                nullable = true
        )
        @Size(max = 255) String isuFldName
) {
}
