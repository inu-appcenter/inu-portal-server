package kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.TimeTableItemType;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.Visibility;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTableItem;

public record TimeTableDetailItemResponseDto(
        @Schema(description = "시간표 요소 id", example = "11")
        Long id,
        @Schema(description = "시간표 요소 타입", example = "COURSE")
        TimeTableItemType type,
        @Schema(description = "시간표 요소 메모", example = "중간고사 4/22, 기말고사 6/17")
        String memo,
        @Schema(description = "강의 기반 시간표 요소 상세 정보. type이 COURSE일 때 값이 존재합니다.", nullable = true)
        CourseTimeTableItemResponseDto course,
        @Schema(description = "커스텀 일정 기반 시간표 요소 상세 정보. type이 CUSTOM일 때 값이 존재합니다.", nullable = true)
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

    // 강의 시간표 요소 (친구용)
    public static TimeTableDetailItemResponseDto ofCourse(
            TimeTableItem item,
            CourseTimeTableItemResponseDto course,
            Visibility visibility
    ) {
        boolean masked = visibility == Visibility.PROTECTED;

        return new TimeTableDetailItemResponseDto(
                masked ? null : item.getId(),
                item.getType(),
                // memo는 본인만 보는 개인 메모이므로 공개범위와 무관하게 친구에게는 항상 비공개
                null,
                course,
                null
        );
    }

    // 커스텀일정 시간표 요소 (친구용)
    public static TimeTableDetailItemResponseDto ofCustom(
            TimeTableItem item,
            CustomTimeTableItemResponseDto customSchedule,
            Visibility visibility
    ) {
        boolean masked = visibility == Visibility.PROTECTED;

        return new TimeTableDetailItemResponseDto(
                masked ? null : item.getId(),
                item.getType(),
                // memo는 본인만 보는 개인 메모이므로 공개범위와 무관하게 친구에게는 항상 비공개
                null,
                null,
                customSchedule
        );
    }
}
