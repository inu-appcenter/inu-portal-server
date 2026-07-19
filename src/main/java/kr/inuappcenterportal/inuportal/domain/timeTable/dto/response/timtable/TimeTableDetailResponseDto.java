package kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timtable;

import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem.TimeTableDetailItemResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;

import java.util.List;

public record TimeTableDetailResponseDto(
        Long id,
        String timeTableName,
        Integer year,
        SemesterTerm term,
        List<TimeTableDetailItemResponseDto> items
) {
    public static TimeTableDetailResponseDto from(
            TimeTable timeTable,
            List<TimeTableDetailItemResponseDto> items
    ) {
        return new TimeTableDetailResponseDto(
                timeTable.getId(),
                timeTable.getTimeTableName(),
                timeTable.getSemester().getYear(),
                timeTable.getSemester().getTerm(),
                items
        );
    }
}
