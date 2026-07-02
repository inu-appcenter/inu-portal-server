package kr.inuappcenterportal.inuportal.domain.semester.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;

import java.time.LocalDate;

public record SemesterResponseDto(

        @Schema(description = "학기 ID", example = "1")
        Long id,

        @Schema(description = "년도", example = "2026")
        Integer year,

        @Schema(description = "학기", example = "FIRST")
        SemesterTerm term,

        @Schema(description = "학기 상태", example = "OPEN")
        SemesterStatus status,

        @Schema(description = "학기 시작일", example = "2026-03-02")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @Schema(description = "학기 종료일", example = "2026-06-21")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate) {

    public static SemesterResponseDto from(Semester semester) {
        return new SemesterResponseDto(
                semester.getId(),
                semester.getYear(),
                semester.getTerm(),
                semester.getStatus(),
                semester.getStartDate(),
                semester.getEndDate()
        );
    }
}
