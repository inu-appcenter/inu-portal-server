package kr.inuappcenterportal.inuportal.domain.course.enums.courseMeeting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalTime;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum TimeMapper {
    P0("0", "08:00", "08:50"),
    P1("1", "09:00", "09:50"),
    P2("2", "10:00", "10:50"),
    P3("3", "11:00", "11:50"),
    P4("4", "12:00", "12:50"),
    P5("5", "13:00", "13:50"),
    P6("6", "14:00", "14:50"),
    P7("7", "15:00", "15:50"),
    P8("8", "16:00", "16:50"),
    P9("9", "17:00", "17:50"),

    NIGHT1("야1", "18:00", "18:50"),
    NIGHT2("야2", "18:55", "19:45"),
    NIGHT3("야3", "19:50", "20:40"),
    NIGHT4("야4", "20:45", "21:35"),
    NIGHT5("야5", "21:40", "22:30"),

    P0A_0("0A~0", "07:30", "08:45"),
    P1_2A("1~2A", "09:00", "10:15"),
    P2B_3("2B~3", "10:30", "11:45"),
    P4_5A("4~5A", "12:00", "13:15"),
    P5B_6("5B~6", "13:30", "14:45"),
    P7_8A("7~8A", "15:00", "16:15"),
    P8B_9("8B~9", "16:30", "17:45"),

    NIGHT1_2A("야1~2A", "18:00", "19:15"),
    NIGHT2B_3("야2B~3", "19:25", "20:40"),
    NIGHT4_5A("야4~5A", "20:50", "22:05");

    private final String code;
    private final String start;
    private final String end;

    public static TimeMapper from(String value) {
        String normalized = normalize(value);

        return Arrays.stream(values())
                .filter(period -> period.code.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 교시: " + value));
    }

    public static String normalize(String value) {
        return value.trim()
                .replace(" ", "")
                .replace("-", "~")
                .replace("∼", "~")
                .replace("～", "~");
    }

    public LocalTime startTime() {
        return LocalTime.parse(start);
    }

    public LocalTime endTime() {
        return LocalTime.parse(end);
    }
}
