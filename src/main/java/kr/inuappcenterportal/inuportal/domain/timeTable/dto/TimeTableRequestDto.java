package kr.inuappcenterportal.inuportal.domain.timeTable.dto;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.Visibility;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;

public record TimeTableRequestDto(
        Long id,
        String timeTableName,
        boolean isPrimary,
        Visibility visibility
) {
    public TimeTable toEntity(Member member, Semester semester) {
        return TimeTable.create(
                timeTableName,
                isPrimary,
                member,
                semester
        );
    }
}
