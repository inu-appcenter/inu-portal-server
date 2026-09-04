package kr.inuappcenterportal.inuportal.domain.firebase.scheduler;

import kr.inuappcenterportal.inuportal.domain.firebase.event.ScheduledNotificationDueEvent;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.ScheduledNotificationRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.ScheduledNotificationTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 도래한 예약 알림을 선점해 발송 이벤트만 발행한다. 실제 발송은
 * {@link kr.inuappcenterportal.inuportal.domain.firebase.event.ScheduledNotificationListener}가
 * 담당한다 (FcmStalledNotificationScheduler ↔ FcmEventListener와 동일한 역할 분리).
 * <p>
 * 이 메서드에는 {@code @Transactional}을 붙이지 않는다. 상태 전이는 전부
 * {@link ScheduledNotificationTransactionService}(별도 빈, REQUIRES_NEW)가 담당하므로,
 * 이 메서드 자체가 트랜잭션을 가질 필요가 없다 — 자기 호출로 프록시를 우회해 트랜잭션이
 * 누락되는 사고(FcmStalledNotificationScheduler에 있는 것과 같은 종류)를 설계로 차단한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledNotificationScheduler {

    private final ScheduledNotificationRepository scheduledNotificationRepository;
    private final ScheduledNotificationTransactionService txService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /** 기본값 false. 명시적으로 켜기 전까지는 아무 것도 하지 않는다. */
    @Value("${fcm.schedule.enabled:false}")
    private boolean scheduleEnabled;

    /** 한 번의 실행에서 발행할 최대 건수. 대량 예약이 몰려도 부하가 튀지 않도록 제한한다. */
    @Value("${fcm.schedule.max-per-run:20}")
    private int maxPerRun;

    @Scheduled(fixedDelayString = "${fcm.schedule.interval-ms:60000}")
    @SchedulerLock(
            name = "scheduled-notification-dispatch",
            lockAtMostFor = "PT50S",
            lockAtLeastFor = "PT10S"
    )
    public void dispatchDueNotifications() {
        if (!scheduleEnabled) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Pageable pageable = PageRequest.of(0, maxPerRun);
        List<Long> dueIds = scheduledNotificationRepository.findDueIds(now, pageable);

        if (dueIds.isEmpty()) {
            return;
        }

        int dispatched = 0;
        for (Long id : dueIds) {
            // 원자적 선점: 이미 취소되었거나(다른 요청이 CANCELED로 바꿨거나) 다른 인스턴스가
            // 먼저 집었다면 lease가 false를 반환하고, 이 행은 건드리지 않는다.
            if (!txService.lease(id)) {
                continue;
            }
            eventPublisher.publishEvent(new ScheduledNotificationDueEvent(id));
            dispatched++;
        }

        if (dispatched > 0) {
            log.info("Scheduled notification dispatch finished: dispatched={}", dispatched);
        }
    }
}
