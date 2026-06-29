package kr.inuappcenterportal.inuportal.domain.semester.dto;

import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class SemesterResponseDto {

    private Long id;
    private Integer year;
    private SemesterTerm term;
    private SemesterStatus status;
    private LocalDate startDate;
    private LocalDate endDate;

    public static SemesterResponseDto of(Semester semester) {
        return SemesterResponseDto.builder()
                .id(semester.getId())
                .year(semester.getYear())
                .term(semester.getTerm())
                .status(semester.getStatus())
                .startDate(semester.getStartDate())
                .endDate(semester.getEndDate())
                .build();
    }
}
