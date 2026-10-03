package kr.inuappcenterportal.inuportal.domain.firebase.dto;

/**
 * iOS ActivityKit push-to-start 한 건의 내용.
 *
 * <p>앱은 expo-widgets로 Live Activity를 그리는데, 그 ActivityAttributes는
 * {@code LiveActivityAttributes}(필드 없음)이고 ContentState는 {@code { name, props }} 하나뿐이다.
 * {@code props}는 레이아웃 컴포넌트가 받는 props 객체를 JSON <b>문자열</b>로 직렬화한 값이다.
 *
 * @param activityName  앱의 createLiveActivity 이름 (ContentState.name)
 * @param propsJson     레이아웃 props의 JSON 문자열 (ContentState.props)
 * @param staleDateSec  이 시각 이후 내용이 낡은 것으로 표시된다 (epoch seconds)
 * @param expirationSec APNs가 이 시각까지만 전달을 재시도한다 (epoch seconds)
 */
public record LiveActivityStartPush(
        String activityName,
        String propsJson,
        long staleDateSec,
        long expirationSec
) {
    /** 앱 쪽 ActivityAttributes 타입 이름 (expo-widgets WidgetLiveActivity.swift). */
    public static final String ATTRIBUTES_TYPE = "LiveActivityAttributes";

    /** 앱의 시간표 Live Activity 이름 (createLiveActivity, ContentState.name). */
    public static final String TIMETABLE_ACTIVITY_NAME = "TimetableLiveActivity";
}
