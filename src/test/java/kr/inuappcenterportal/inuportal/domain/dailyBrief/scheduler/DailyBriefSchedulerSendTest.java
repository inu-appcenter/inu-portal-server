package kr.inuappcenterportal.inuportal.domain.dailyBrief.scheduler;

import kr.inuappcenterportal.inuportal.domain.firebase.dto.LiveActivityStartPush;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.scheduler.LiveActivityLifecycleScheduler;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyBriefSchedulerSendTest {

    private static final LiveActivityStartPush PUSH =
            new LiveActivityStartPush("TimetableLiveActivity", "{}", 1_800_000_000_000L, 1_800_000_000L, 1_800_000_000L);

    @Mock
    private DailyBriefPlanner dailyBriefPlanner;
    @Mock
    private FcmService fcmService;
    @Mock
    private LiveActivityLifecycleScheduler liveActivityLifecycleScheduler;

    @InjectMocks
    private DailyBriefScheduler dailyBriefScheduler;

    @Test
    void preClassAlert_sendsWithLiveActivity() {
        when(dailyBriefPlanner.planPreClassAlerts()).thenReturn(List.of(
                new BriefNotification(1L, "10분 후 수업이 시작돼요.", "자료구조", FcmMessageType.DAILY_BRIEF_TIMETABLE, "/timetable", PUSH)));

        dailyBriefScheduler.sendPreClassAlerts();

        verify(fcmService).sendPreClassNotification(1L, "10분 후 수업이 시작돼요.", "자료구조",
                FcmMessageType.DAILY_BRIEF_TIMETABLE, "/timetable", PUSH);
        verify(fcmService, never()).sendDailyBriefNotification(any(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void preClassAlert_endsEarlierActivitiesOnDevicesThatGotTheNewOne() {
        when(dailyBriefPlanner.planPreClassAlerts()).thenReturn(List.of(
                new BriefNotification(1L, "10분 후 수업이 시작돼요.", "자료구조", FcmMessageType.DAILY_BRIEF_TIMETABLE, "/timetable", PUSH)));
        when(fcmService.sendPreClassNotification(eq(1L), anyString(), anyString(), any(), anyString(), eq(PUSH)))
                .thenReturn(Set.of("ios-device"));

        dailyBriefScheduler.sendPreClassAlerts();

        verify(liveActivityLifecycleScheduler).endEarlierActivities(1L, Set.of("ios-device"), PUSH.classStartMs());
    }

    @Test
    void preClassAlert_keepsEarlierActivitiesWhenNoDeviceGotALiveActivity() {
        // 일반 알림만 받은 기기(iOS 17.2 미만 등)는 새 Activity가 없으므로 앞 수업 것을 남긴다.
        when(dailyBriefPlanner.planPreClassAlerts()).thenReturn(List.of(
                new BriefNotification(1L, "t", "b", FcmMessageType.DAILY_BRIEF_TIMETABLE, "/timetable", PUSH)));
        when(fcmService.sendPreClassNotification(eq(1L), anyString(), anyString(), any(), anyString(), eq(PUSH)))
                .thenReturn(Set.of());

        dailyBriefScheduler.sendPreClassAlerts();

        verify(liveActivityLifecycleScheduler, never()).endEarlierActivities(any(), any(), anyLong());
    }

    @Test
    void oneMemberFailure_doesNotStopOthers() {
        when(dailyBriefPlanner.planDailyScheduleBriefing()).thenReturn(List.of(
                new BriefNotification(1L, "t", "b1", FcmMessageType.DAILY_BRIEF_SCHEDULE, "/home/calendar", null),
                new BriefNotification(2L, "t", "b2", FcmMessageType.DAILY_BRIEF_SCHEDULE, "/home/calendar", null)));
        doThrow(new RuntimeException("boom")).when(fcmService)
                .sendDailyBriefNotification(eq(1L), anyString(), anyString(), any(), anyString());

        dailyBriefScheduler.sendDailyScheduleBriefing();

        verify(fcmService).sendDailyBriefNotification(2L, "t", "b2", FcmMessageType.DAILY_BRIEF_SCHEDULE, "/home/calendar");
    }
}
