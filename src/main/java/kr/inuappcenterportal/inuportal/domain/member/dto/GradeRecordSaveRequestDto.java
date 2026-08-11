package kr.inuappcenterportal.inuportal.domain.member.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;

import java.util.List;

public record GradeRecordSaveRequestDto(
        @NotNull Integer year,
        @NotNull SemesterTerm term,
        @NotEmpty List<@Valid GradeRecordRequestDto> records
) {
}
