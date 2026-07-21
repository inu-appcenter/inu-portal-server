package kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem;

import kr.inuappcenterportal.inuportal.domain.course.enums.DayOfWeek;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomScheduleMeeting;

import java.time.LocalTime;

public record TimeTableMeetingResponseDto(
        Long id,
        String location,
        Integer sequence,
        DayOfWeek day,
        LocalTime startTime,
        LocalTime endTime
) {

    // 강의 시간표 요소
    // CourseMeeting을 TimeTableMeetingResponseDto으로 바꾸는 정적 팩토리 메서드
    public static TimeTableMeetingResponseDto from(CourseMeeting meeting) {
        return new TimeTableMeetingResponseDto(
                meeting.getId(),
                meeting.getLocation(),
                meeting.getSequence(),
                meeting.getDay(),
                meeting.getStartTime(),
                meeting.getEndTime()
        );
    }

    // 커스텀 일정 시간표 요소
    public static TimeTableMeetingResponseDto from(CustomScheduleMeeting meeting) {
        return new TimeTableMeetingResponseDto(
                meeting.getId(),
                meeting.getLocation(),
                null,
                meeting.getDay(),
                meeting.getStartTime(),
                meeting.getEndTime()
        );
    }
}
