package kr.inuappcenterportal.inuportal.domain.firebase.scheduler;

import kr.inuappcenterportal.inuportal.domain.firebase.dto.DueLiveActivity;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.firebase.service.LiveActivityLifecycleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.function.LongSupplier;

/**
 * iOS 시간표 Live Activity를 수업 시각에 맞춰 갱신하고 끝낸다.
 *
 * <ul>
 *   <li>수업 시작 시각이 지나면 {@code update}로 "수업 중"(phase=ONGOING)으로 바꾼다.</li>
 *   <li>수업 종료 시각이 지나면 {@code end}로 끝내고 잠금화면에서도 바로 치운다(dismissal-date=지금).</li>
 * </ul>
 * 앱이 꺼져 있으면 앱은 이 일을 할 수 없어, 끝난 수업의 Activity가 남아 다음 수업 것과 겹쳤다.
 * 연강이면 다음 수업 알림이 앞 수업 종료보다 먼저 오므로, 그때 앞 수업 것도 바로 끝낸다
 * ({@link #endEarlierActivities}).
 *
 * <p>트랜잭션을 걸지 않는다. 대상 조회와 상태 기록은 {@link LiveActivityLifecycleService}의 짧은 트랜잭션,
 * FCM 발송은 그 밖에서 한다.
 */
@Slf4j
@Component
public class LiveActivityLifecycleScheduler {

    static final String PHASE_ONGOING = "ONGOING";
    /** 발송이 실패하면 이 시간 동안은 다음 실행에서 다시 시도하고, 지나면 그 단계를 포기한다. */
    static final long RETRY_WINDOW_MILLIS = 5 * 60_000L;
    /** 끝난 행을 지우기 전에 남겨 두는 시간 (문의 대응용) */
    static final long RETENTION_MILLIS = 24 * 60 * 60_000L;

    private final LiveActivityLifecycleService liveActivityLifecycleService;
    private final FcmService fcmService;
    private final LongSupplier clock;

    @Autowired
    public LiveActivityLifecycleScheduler(LiveActivityLifecycleService liveActivityLifecycleService, FcmService fcmService) {
        this(liveActivityLifecycleService, fcmService, System::currentTimeMillis);
    }

    /** 테스트에서 시각을 고정하기 위한 생성자. */
    LiveActivityLifecycleScheduler(LiveActivityLifecycleService liveActivityLifecycleService, FcmService fcmService,
                                   LongSupplier clock) {
        this.liveActivityLifecycleService = liveActivityLifecycleService;
        this.fcmService = fcmService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void run() {
        long now = clock.getAsLong();
        for (DueLiveActivity activity : liveActivityLifecycleService.findDue(now)) {
            boolean ending = now >= activity.endAt();
            if (!ending && activity.ongoingSent()) {
                continue;
            }
            try {
                String props = LiveActivityLifecycleService.withPhase(activity.propsJson(), PHASE_ONGOING);
                if (ending) {
                    fcmService.sendLiveActivityEvent(activity.fcmToken(), activity.pushToken(), "end", props,
                            null, now / 1000);
                    liveActivityLifecycleService.markEnded(activity.id());
                } else {
                    fcmService.sendLiveActivityEvent(activity.fcmToken(), activity.pushToken(), "update", props,
                            activity.endAt() / 1000, null);
                    liveActivityLifecycleService.markOngoingSent(activity.id());
                }
            } catch (Exception e) {
                long stageAt = ending ? activity.endAt() : activity.startAt();
                boolean giveUp = now - stageAt >= RETRY_WINDOW_MILLIS;
                log.warn("Live Activity {} failed (id={}, giveUp={}): {}",
                        ending ? "end" : "update", activity.id(), giveUp, e.getMessage());
                if (giveUp) {
                    if (ending) {
                        liveActivityLifecycleService.markEnded(activity.id());
                    } else {
                        liveActivityLifecycleService.markOngoingSent(activity.id());
                    }
                }
            }
        }
        liveActivityLifecycleService.deleteEndedBefore(now - RETENTION_MILLIS);
    }

    /**
     * 다음 수업 Live Activity를 막 시작한 기기들에서, 그보다 먼저 시작한 수업의 Activity를 끝낸다.
     *
     * <p>연강이면 다음 수업 알림(수업 N분 전)이 앞 수업 종료보다 먼저 와서 잠금화면에 카드가 둘 쌓인다.
     * 사용자의 관심은 이미 다음 수업으로 넘어갔으므로 앞 수업 것은 바로 치운다. 발송이 실패하면 상태를
     * 남겨 두어, 원래대로 수업 종료 시각에 {@link #run()}이 끝낸다.
     *
     * @param fcmTokens    다음 수업 Live Activity 시작 푸시를 받은 기기들
     * @param classStartMs 다음 수업 시작 시각
     */
    public void endEarlierActivities(Long memberId, Collection<String> fcmTokens, long classStartMs) {
        long now = clock.getAsLong();
        for (DueLiveActivity activity : liveActivityLifecycleService.findEarlierActive(memberId, fcmTokens, classStartMs)) {
            try {
                String props = LiveActivityLifecycleService.withPhase(activity.propsJson(), PHASE_ONGOING);
                fcmService.sendLiveActivityEvent(activity.fcmToken(), activity.pushToken(), "end", props,
                        null, now / 1000);
                liveActivityLifecycleService.markEnded(activity.id());
            } catch (Exception e) {
                log.warn("Live Activity end on next class failed (id={}): {}", activity.id(), e.getMessage());
            }
        }
    }
}
