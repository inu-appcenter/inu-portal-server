package kr.inuappcenterportal.inuportal.domain.semester.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;

import java.time.LocalDate;

public record SemesterResponseDto(
        Long id,
        Integer year,
        SemesterTerm term,
        SemesterStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,
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
