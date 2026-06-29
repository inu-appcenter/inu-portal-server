package kr.inuappcenterportal.inuportal.domain.semester.dto;

import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;

import java.time.LocalDate;

public record SemesterResponseDto(
        Long id,
        Integer year,
        SemesterTerm term,
        SemesterStatus status,
        LocalDate startDate,
        LocalDate endDate) {

    public static SemesterResponseDto of(Semester semester) {
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
