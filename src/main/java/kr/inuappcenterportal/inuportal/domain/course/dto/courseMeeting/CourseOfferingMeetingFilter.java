package kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting;

import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.DayOfWeek;

import java.time.LocalTime;

public record CourseOfferingMeetingFilter(
        DayOfWeek day,
        LocalTime startTime,
        LocalTime endTime
) {
}
