package kr.inuappcenterportal.inuportal.domain.schedulePoll.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;

// 정기 조율의 열 키, 일회성 조율의 요일 거르기. 선언 순서(월→일)가 그리드의 열 순서다.
@Getter
@RequiredArgsConstructor
public enum PollDay {
    MON("월"),
    TUE("화"),
    WED("수"),
    THU("목"),
    FRI("금"),
    SAT("토"),
    SUN("일");

    private final String label;

    // 선언 순서가 월→일이라 DayOfWeek(1=월 … 7=일)와 같다.
    public DayOfWeek toDayOfWeek() {
        return DayOfWeek.of(ordinal() + 1);
    }
}
