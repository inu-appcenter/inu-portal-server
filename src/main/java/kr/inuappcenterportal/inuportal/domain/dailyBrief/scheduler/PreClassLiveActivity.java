package kr.inuappcenterportal.inuportal.domain.dailyBrief.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.LiveActivityStartPush;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 수업 시작 전 알림에 실을 iOS 시간표 Live Activity 시작 내용을 만든다.
 *
 * <p>props 형태는 앱의 {@code TimetableLiveActivityProps}
 * (intip-mobile-app {@code src/widgets/TimetableLiveActivity.tsx})와 일치해야 한다.
 * 시각은 모두 epoch milliseconds다.
 */
final class PreClassLiveActivity {

    /** 앱의 createLiveActivity 이름. */
    static final String ACTIVITY_NAME = LiveActivityStartPush.TIMETABLE_ACTIVITY_NAME;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PreClassLiveActivity() {
    }

    static LiveActivityStartPush of(String courseTitle, String location, LocalDate date, LocalTime startTime,
                                    LocalTime endTime, int alertMinutes, ZoneId zone) {
        long startMs = date.atTime(startTime).atZone(zone).toInstant().toEpochMilli();
        long endMs = date.atTime(endTime).atZone(zone).toInstant().toEpochMilli();

        // 레이아웃에서 undefined와 null을 구분하지 않도록 빈 값은 아예 싣지 않는다.
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("phase", "UPCOMING");
        props.put("courseTitle", courseTitle);
        if (location != null && !location.isBlank()) {
            props.put("location", location);
        }
        props.put("startTimestamp", startMs);
        props.put("endTimestamp", endMs);
        props.put("countdownFromTimestamp", startMs - alertMinutes * 60_000L);
        props.put("durationMinutes", Math.max(1, (endMs - startMs) / 60_000L));

        String propsJson;
        try {
            propsJson = OBJECT_MAPPER.writeValueAsString(props);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Live Activity props 직렬화 실패", e);
        }

        // 서버는 수업이 시작돼도 상태를 갱신하지 않는다(업데이트 토큰이 없다). 시작 시각을 stale-date로
        // 두면 시스템이 그때 뷰를 다시 그리고, 레이아웃은 현재 시각으로 "수업 중"을 판단한다.
        // 수업이 시작된 뒤에 도착하는 시작 푸시는 의미가 없으므로 APNs 전달 시한도 시작 시각으로 둔다.
        long startSec = startMs / 1000;
        return new LiveActivityStartPush(ACTIVITY_NAME, propsJson, startSec, startSec);
    }
}
