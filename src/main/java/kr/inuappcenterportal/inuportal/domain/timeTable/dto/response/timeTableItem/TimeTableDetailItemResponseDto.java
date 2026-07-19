package kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem;

import kr.inuappcenterportal.inuportal.domain.timeTable.enums.TimeTableItemType;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTableItem;

public record TimeTableDetailItemResponseDto(
        Long id,
        TimeTableItemType type,
        String memo,
        CourseTimeTableItemResponseDto course,
        CustomTimeTableItemResponseDto customSchedule
) {
    // 강의 시간표 요소
    public static TimeTableDetailItemResponseDto ofCourse(
            TimeTableItem item,
            CourseTimeTableItemResponseDto course
    ) {
        return new TimeTableDetailItemResponseDto(
                item.getId(),
                item.getType(),
                item.getMemo(),
                course,
                null
        );
    }

    // 커스텀일정 시간표 요소
    public static TimeTableDetailItemResponseDto ofCustom(
            TimeTableItem item,
            CustomTimeTableItemResponseDto customSchedule
    ) {
        return new TimeTableDetailItemResponseDto(
                item.getId(),
                item.getType(),
                item.getMemo(),
                null,
                customSchedule
        );
    }

}
