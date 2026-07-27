package kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timtable;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem.TimeTableDetailItemResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;

import java.util.List;

public record TimeTableDetailResponseDto(
        @Schema(description = "시간표 id", example = "1")
        Long id,
        @Schema(description = "시간표 이름", example = "1학기 기본 시간표")
        String timeTableName,
        @Schema(description = "학년도", example = "2026")
        Integer year,
        @Schema(description = "학기", example = "FIRST")
        SemesterTerm term,
        @Schema(description = "시간표 요소 상세 목록")
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
