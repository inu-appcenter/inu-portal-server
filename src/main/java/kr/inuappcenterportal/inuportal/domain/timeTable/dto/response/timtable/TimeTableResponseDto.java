package kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timtable;

import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.Visibility;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;

public record TimeTableResponseDto(
        Long id,
        Long semesterId,
        Integer year,
        SemesterTerm term,
        String timeTableName,
        boolean isPrimary,
        Visibility visibility
) {
    public static TimeTableResponseDto from(TimeTable timeTable) {
        return new TimeTableResponseDto(
                timeTable.getId(),
                timeTable.getSemester().getId(),
                timeTable.getSemester().getYear(),
                timeTable.getSemester().getTerm(),
                timeTable.getTimeTableName(),
                timeTable.isPrimary(),
                timeTable.getVisibility()
        );
    }
}
