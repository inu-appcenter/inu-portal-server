package kr.inuappcenterportal.inuportal.domain.schedulePoll.service;

import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.AvailableRangeDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.PollDay;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.exception.SchedulePollErrorCode;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.exception.SchedulePollException;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePoll;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// 가능 시간 구간 검증·정리. DB와 무관한 계산만 한다.
// 시각은 "HH:mm"이고 하루의 끝은 "24:00"(1440분)이라 LocalTime 대신 분 숫자로 다룬다.
public class SchedulePollSlotUtils {

    public static final int SLOT_MINUTES = 30;
    public static final int DAY_END_MINUTES = 24 * 60;

    private SchedulePollSlotUtils() {
    }

    // 30분 단위가 아니거나 00:00~24:00 밖이면 IllegalArgumentException
    public static int toMinutes(String time) {
        if (time == null || !time.matches("\\d{2}:\\d{2}")) {
            throw new IllegalArgumentException("시각 형식이 아닙니다: " + time);
        }
        int hour = Integer.parseInt(time.substring(0, 2));
        int minute = Integer.parseInt(time.substring(3, 5));
        if (minute >= 60) {
            throw new IllegalArgumentException("시각 형식이 아닙니다: " + time);
        }
        int minutes = hour * 60 + minute;
        if (minutes > DAY_END_MINUTES || minutes % SLOT_MINUTES != 0) {
            throw new IllegalArgumentException("30분 단위 시각이 아닙니다: " + time);
        }
        return minutes;
    }

    public static String formatMinutes(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    // 조율의 열 키. 정기면 고른 요일(월→일 순), 일회성이면 시작일~종료일 날짜("YYYY-MM-DD") 전부
    public static List<String> columnKeys(SchedulePoll poll) {
        if (poll.isRecurring()) {
            return poll.getDays().stream().sorted().distinct().map(PollDay::name).toList();
        }
        List<String> keys = new ArrayList<>();
        for (LocalDate date = poll.getStartDate(); !date.isAfter(poll.getEndDate()); date = date.plusDays(1)) {
            keys.add(date.toString());
        }
        return keys;
    }

    // 제출된 구간을 검증하고 저장할 모양으로 정리한다.
    // - key가 조율의 열이 아니거나, 30분 단위가 아니거나, rangeStart ≤ start < end ≤ rangeEnd를 어기면 400 INVALID_SLOT
    // - 같은 열에서 겹치거나 붙은 구간은 하나로 합친다
    // - 결과는 열 순서, 시작 시각 순으로 정렬한다
    public static List<AvailableRangeDto> normalize(SchedulePoll poll, List<AvailableRangeDto> ranges) {
        List<String> columns = columnKeys(poll);
        List<int[]> parsed = new ArrayList<>(); // {열 번호, 시작 분, 끝 분}
        for (AvailableRangeDto range : ranges) {
            if (range == null || range.key() == null) {
                throw new SchedulePollException(SchedulePollErrorCode.INVALID_SLOT);
            }
            int column = columns.indexOf(range.key());
            int start = parseSlotTime(range.start());
            int end = parseSlotTime(range.end());
            if (column < 0 || start < poll.getRangeStartMinutes() || start >= end || end > poll.getRangeEndMinutes()) {
                throw new SchedulePollException(SchedulePollErrorCode.INVALID_SLOT);
            }
            parsed.add(new int[]{column, start, end});
        }
        parsed.sort(Comparator.<int[]>comparingInt(r -> r[0]).thenComparingInt(r -> r[1]));

        List<AvailableRangeDto> merged = new ArrayList<>();
        int[] current = null;
        for (int[] range : parsed) {
            if (current != null && current[0] == range[0] && range[1] <= current[2]) {
                current[2] = Math.max(current[2], range[2]);
                continue;
            }
            if (current != null) {
                merged.add(toDto(columns, current));
            }
            current = range;
        }
        if (current != null) {
            merged.add(toDto(columns, current));
        }
        return merged;
    }

    private static int parseSlotTime(String time) {
        try {
            return toMinutes(time);
        } catch (IllegalArgumentException e) {
            throw new SchedulePollException(SchedulePollErrorCode.INVALID_SLOT);
        }
    }

    private static AvailableRangeDto toDto(List<String> columns, int[] range) {
        return new AvailableRangeDto(columns.get(range[0]), formatMinutes(range[1]), formatMinutes(range[2]));
    }
}
