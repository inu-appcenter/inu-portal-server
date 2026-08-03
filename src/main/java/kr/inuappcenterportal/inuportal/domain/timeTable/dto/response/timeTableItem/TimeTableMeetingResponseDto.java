package kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.course.enums.course.DayOfWeek;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomScheduleMeeting;

import java.time.LocalTime;

public record TimeTableMeetingResponseDto(
        @Schema(description = "강의 시간 또는 커스텀 일정 시간 id", example = "21")
        Long id,
        @Schema(description = "장소", example = "07-504")
        String location,
        @Schema(description = "강의 시간 순서. 커스텀 일정은 null입니다.", example = "1", nullable = true)
        String sequence,
        @Schema(description = "요일", example = "MONDAY")
        DayOfWeek day,
        @Schema(description = "시작 시간", type = "string", example = "09:00", pattern = "HH:mm")
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,
        @Schema(description = "종료 시간", type = "string", example = "10:15", pattern = "HH:mm")
        @JsonFormat(pattern = "HH:mm")
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

    public static TimeTableMeetingResponseDto from(CourseMeeting meeting, boolean masked) {
        return new TimeTableMeetingResponseDto(
                masked ? null : meeting.getId(),
                masked ? null : meeting.getLocation(),
                masked ? null : meeting.getSequence(),
                meeting.getDay(),
                meeting.getStartTime(),
                meeting.getEndTime()
        );
    }

    public static TimeTableMeetingResponseDto from(CustomScheduleMeeting meeting, boolean masked) {
        return new TimeTableMeetingResponseDto(
                masked ? null : meeting.getId(),
                masked ? null : meeting.getLocation(),
                null,
                meeting.getDay(),
                meeting.getStartTime(),
                meeting.getEndTime()
        );
    }
}
