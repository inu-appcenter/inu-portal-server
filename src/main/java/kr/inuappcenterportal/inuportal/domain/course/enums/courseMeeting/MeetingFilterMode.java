package kr.inuappcenterportal.inuportal.domain.course.enums.courseMeeting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MeetingFilterMode {
    HAS_CLASS("시간대 필터"),
    NO_CLASS("공강 필터");

    private final String description;
}
