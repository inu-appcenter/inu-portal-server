package kr.inuappcenterportal.inuportal.domain.dailyBrief.scheduler;

import kr.inuappcenterportal.inuportal.domain.firebase.scheduler.LiveActivityLifecycleScheduler;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Daily Brief 알림 스케줄러.
 *
 * <p>일부러 트랜잭션을 걸지 않는다. 대상과 내용은 {@link DailyBriefPlanner}가 짧은 read-only 트랜잭션에서
 * 값으로 계산해 돌려주고, 여기서는 그 목록을 트랜잭션 밖에서 발송한다. FCM 발송(외부 HTTP)이 도는 동안
 * DB 커넥션을 붙잡지 않기 위해서다. 발송 이력 저장은 {@link FcmService}가 회원별 짧은 쓰기 트랜잭션으로 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyBriefScheduler {

    private final DailyBriefPlanner dailyBriefPlanner;
    private final FcmService fcmService;
    private final LiveActivityLifecycleScheduler liveActivityLifecycleScheduler;

    /**
     * 1. 수업 시작 전 알림 (5분마다 실행)
     */
    @Scheduled(cron = "0 */5 * * * MON-FRI", zone = "Asia/Seoul")
    public void sendPreClassAlerts() {
        send(dailyBriefPlanner.planPreClassAlerts(), "pre-class alert");
    }

    /**
     * 2. 당일 강의 목록 묶음 브리핑 (10분마다 실행)
     */
    @Scheduled(cron = "0 0/10 7-22 * * MON-FRI", zone = "Asia/Seoul")
    public void sendDailyClassBriefing() {
        send(dailyBriefPlanner.planDailyClassBriefing(), "daily class brief");
    }

    /**
     * 3. 당일 학사/학과 일정 브리핑 (10분마다 실행)
     */
    @Scheduled(cron = "0 0/10 7-22 * * *", zone = "Asia/Seoul")
    public void sendDailyScheduleBriefing() {
        send(dailyBriefPlanner.planDailyScheduleBriefing(), "daily schedule brief");
    }

    private void send(List<BriefNotification> notifications, String kind) {
        for (BriefNotification notification : notifications) {
            try {
                if (notification.liveActivity() != null) {
                    Set<String> liveActivityDevices = fcmService.sendPreClassNotification(notification.memberId(),
                            notification.title(), notification.body(), notification.type(), notification.path(),
                            notification.liveActivity());
                    // 연강이면 앞 수업 Live Activity가 아직 떠 있다 — 다음 수업 것을 띄운 기기에서는 바로 치운다.
                    if (!liveActivityDevices.isEmpty()) {
                        liveActivityLifecycleScheduler.endEarlierActivities(notification.memberId(),
                                liveActivityDevices, notification.liveActivity().classStartMs());
                    }
                } else {
                    fcmService.sendDailyBriefNotification(notification.memberId(), notification.title(),
                            notification.body(), notification.type(), notification.path());
                }
            } catch (Exception e) {
                log.error("Failed to send {} for memberId={}: {}", kind, notification.memberId(), e.getMessage(), e);
            }
        }
    }
}
