package kr.inuappcenterportal.inuportal.domain.dailyBrief.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.LiveActivityStartPush;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PreClassLiveActivityTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Test
    @DisplayName("앱 TimetableLiveActivityProps 형태로 props를 만들고 stale/전달 시한을 수업 시작으로 둔다")
    void buildsAppProps() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 30);
        LiveActivityStartPush push = PreClassLiveActivity.of(
                "자료구조", "7호관 301", date, LocalTime.of(10, 30), LocalTime.of(11, 45), 10, SEOUL);

        long startMs = ZonedDateTime.of(2026, 9, 30, 10, 30, 0, 0, SEOUL).toInstant().toEpochMilli();
        long endMs = ZonedDateTime.of(2026, 9, 30, 11, 45, 0, 0, SEOUL).toInstant().toEpochMilli();

        assertThat(push.activityName()).isEqualTo("TimetableLiveActivity");
        assertThat(push.staleDateSec()).isEqualTo(startMs / 1000);
        assertThat(push.expirationSec()).isEqualTo(startMs / 1000);

        Map<?, ?> props = new ObjectMapper().readValue(push.propsJson(), Map.class);
        assertThat(props.get("phase")).isEqualTo("UPCOMING");
        assertThat(props.get("courseTitle")).isEqualTo("자료구조");
        assertThat(props.get("location")).isEqualTo("7호관 301");
        assertThat(((Number) props.get("startTimestamp")).longValue()).isEqualTo(startMs);
        assertThat(((Number) props.get("endTimestamp")).longValue()).isEqualTo(endMs);
        assertThat(((Number) props.get("countdownFromTimestamp")).longValue()).isEqualTo(startMs - 10 * 60_000L);
        assertThat(((Number) props.get("durationMinutes")).intValue()).isEqualTo(75);
    }

    @Test
    @DisplayName("강의실이 없으면 location 키를 싣지 않는다")
    void omitsBlankLocation() throws Exception {
        LiveActivityStartPush push = PreClassLiveActivity.of(
                "자료구조", " ", LocalDate.of(2026, 9, 30), LocalTime.of(9, 0), LocalTime.of(10, 0), 10, SEOUL);

        Map<?, ?> props = new ObjectMapper().readValue(push.propsJson(), Map.class);
        assertThat(props.containsKey("location")).isFalse();
    }
}
