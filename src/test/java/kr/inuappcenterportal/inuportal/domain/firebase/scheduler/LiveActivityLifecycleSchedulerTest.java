package kr.inuappcenterportal.inuportal.domain.firebase.scheduler;

import kr.inuappcenterportal.inuportal.domain.firebase.dto.DueLiveActivity;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.firebase.service.LiveActivityLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveActivityLifecycleSchedulerTest {

    private static final long START = 1_800_000_000_000L;
    private static final long END = START + 75 * 60_000L;
    private static final String PROPS = "{\"phase\":\"UPCOMING\",\"startTimestamp\":" + START + ",\"endTimestamp\":" + END + "}";
    private static final String ONGOING_PROPS = "{\"phase\":\"ONGOING\",\"startTimestamp\":" + START + ",\"endTimestamp\":" + END + "}";

    @Mock
    private LiveActivityLifecycleService service;
    @Mock
    private FcmService fcmService;

    private long now;
    private LiveActivityLifecycleScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new LiveActivityLifecycleScheduler(service, fcmService, () -> now);
    }

    private void due(boolean ongoingSent) {
        when(service.findDue(now)).thenReturn(List.of(
                new DueLiveActivity(1L, "fcm", "activity-token", PROPS, START, END, ongoingSent)));
    }

    @Test
    @DisplayName("수업이 시작되면 ONGOING으로 갱신하고 stale-date는 수업 종료로 둔다")
    void updatesToOngoingAtStart() throws Exception {
        now = START + 30_000;
        due(false);

        scheduler.run();

        verify(fcmService).sendLiveActivityEvent("fcm", "activity-token", "update", ONGOING_PROPS, END / 1000, null);
        verify(service).markOngoingSent(1L);
        verify(service, never()).markEnded(anyLong());
    }

    @Test
    @DisplayName("이미 ONGOING을 보냈고 수업 중이면 아무것도 보내지 않는다")
    void skipsWhileOngoing() {
        now = START + 10 * 60_000;
        due(true);

        scheduler.run();

        verifyNoInteractions(fcmService);
    }

    @Test
    @DisplayName("수업이 끝나면 종료하고 잠금화면에서 바로 치운다")
    void endsAtClassEnd() throws Exception {
        now = END + 20_000;
        due(true);

        scheduler.run();

        verify(fcmService).sendLiveActivityEvent("fcm", "activity-token", "end", ONGOING_PROPS, null, now / 1000);
        verify(service).markEnded(1L);
    }

    @Test
    @DisplayName("갱신을 못 보낸 채 수업이 끝났으면 갱신 없이 바로 종료한다")
    void endsEvenIfUpdateWasNeverSent() throws Exception {
        now = END + 20_000;
        due(false);

        scheduler.run();

        verify(fcmService).sendLiveActivityEvent(eq("fcm"), eq("activity-token"), eq("end"), anyString(), any(), any());
        verify(fcmService, never()).sendLiveActivityEvent(anyString(), anyString(), eq("update"), anyString(), any(), any());
        verify(service).markEnded(1L);
    }

    @Test
    @DisplayName("발송이 실패하면 재시도 구간 안에서는 상태를 남겨 다음 실행에서 다시 시도한다")
    void retriesWithinWindow() throws Exception {
        now = END + 60_000;
        due(true);
        doThrow(new RuntimeException("unavailable")).when(fcmService)
                .sendLiveActivityEvent(anyString(), anyString(), anyString(), anyString(), any(), any());

        scheduler.run();

        verify(service, never()).markEnded(anyLong());
    }

    @Test
    @DisplayName("재시도 구간이 지나도 실패하면 그 단계를 포기한다")
    void givesUpAfterWindow() throws Exception {
        now = END + LiveActivityLifecycleScheduler.RETRY_WINDOW_MILLIS;
        due(true);
        doThrow(new RuntimeException("BadDeviceToken")).when(fcmService)
                .sendLiveActivityEvent(anyString(), anyString(), anyString(), anyString(), any(), any());

        scheduler.run();

        verify(service).markEnded(1L);
    }

    @Test
    @DisplayName("끝난 행은 하루 지나면 지운다")
    void deletesOldEndedRows() {
        now = START;
        when(service.findDue(now)).thenReturn(List.of());

        scheduler.run();

        verify(service).deleteEndedBefore(now - LiveActivityLifecycleScheduler.RETENTION_MILLIS);
    }

    @Test
    @DisplayName("다음 수업 Activity를 띄운 기기에서 앞 수업 Activity를 바로 끝낸다")
    void endsEarlierActivitiesForNextClass() throws Exception {
        now = END - 5 * 60_000;   // 앞 수업 종료 5분 전에 다음 수업 알림이 왔다
        long nextStart = END + 60_000;
        when(service.findEarlierActive(7L, Set.of("fcm"), nextStart)).thenReturn(List.of(
                new DueLiveActivity(1L, "fcm", "activity-token", PROPS, START, END, true)));

        scheduler.endEarlierActivities(7L, Set.of("fcm"), nextStart);

        verify(fcmService).sendLiveActivityEvent("fcm", "activity-token", "end", ONGOING_PROPS, null, now / 1000);
        verify(service).markEnded(1L);
    }

    @Test
    @DisplayName("앞 수업 종료 발송이 실패하면 남겨 두어 수업 종료 시각에 다시 끝낸다")
    void keepsEarlierActivityWhenEndFails() throws Exception {
        now = END - 5 * 60_000;
        when(service.findEarlierActive(any(), any(), anyLong())).thenReturn(List.of(
                new DueLiveActivity(1L, "fcm", "activity-token", PROPS, START, END, true)));
        doThrow(new RuntimeException("unavailable")).when(fcmService)
                .sendLiveActivityEvent(anyString(), anyString(), anyString(), anyString(), any(), any());

        scheduler.endEarlierActivities(7L, Set.of("fcm"), END + 60_000);

        verify(service, never()).markEnded(anyLong());
    }
}
