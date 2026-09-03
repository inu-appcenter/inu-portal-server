package kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableImage;

import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.DayOfWeek;

public record TimeTableImageMeetingDto(
        DayOfWeek day,
        String startTime,
        String endTime,
        String classroom
) {
}
