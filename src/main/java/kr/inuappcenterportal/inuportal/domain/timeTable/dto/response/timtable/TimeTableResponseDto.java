package kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timtable;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.Visibility;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;

public record TimeTableResponseDto(
        @Schema(description = "시간표 id", example = "1")
        Long id,
        @Schema(description = "학기 id", example = "3")
        Long semesterId,
        @Schema(description = "학년도", example = "2026")
        Integer year,
        @Schema(description = "학기", example = "FIRST")
        SemesterTerm term,
        @Schema(description = "시간표 이름", example = "1학기 기본 시간표")
        String timeTableName,
        @Schema(description = "대표 시간표 여부. 회원별/학기별로 최대 1개만 true가 될 수 있으며, 대표 시간표가 없을 수도 있습니다.", example = "true")
        boolean isPrimary,
        @Schema(description = "시간표 공개 범위", example = "PUBLIC")
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
