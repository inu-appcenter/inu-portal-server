package kr.inuappcenterportal.inuportal.domain.firebase.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.AdminNotificationDispatch;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.AdminNotificationRequest;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.ScheduledNotificationStatus;
import kr.inuappcenterportal.inuportal.domain.firebase.model.ScheduledNotification;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.ScheduledNotificationRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.firebase.service.ScheduledNotificationTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 예약 알림의 실제 발송을 수행한다.
 * <p>
 * {@code FcmEventListener}(유실 보정)와 달리 {@code @TransactionalEventListener}가 아니라
 * 평범한 {@code @EventListener}를 쓴다. 이벤트는 {@link ScheduledNotificationTransactionService#lease}가
 * 이미 커밋한 뒤에 발행되므로, "롤백된 건은 나가지 않는다"는 AFTER_COMMIT의 목적은 lease
 * 시점에 이미 달성돼 있다. 스케줄러가 자기 자신을 호출하는 구조상 AFTER_COMMIT을 쓰면
 * 프록시를 우회해 트랜잭션이 없는 채로 publish될 위험이 있어 평범한 이벤트를 쓴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledNotificationListener {

    private final ScheduledNotificationRepository scheduledNotificationRepository;
    private final ScheduledNotificationTransactionService txService;
    private final FcmService fcmService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 이 시간(분)보다 오래 지난 예약은 발송하지 않고 EXPIRED로 종결한다. 알림은 시의성이
     * 본질이라, 서버가 내려가 있다가 뒤늦게 올라와 밀린 예약을 한꺼번에 쏘는 사고를 막는다
     * (FcmStalledNotificationScheduler의 max-age-minutes와 같은 목적).
     */
    @Value("${fcm.schedule.max-delay-minutes:30}")
    private long maxDelayMinutes;

    @Async("messageExecutor")
    @EventListener
    public void onDue(ScheduledNotificationDueEvent event) {
        Long id = event.scheduledNotificationId();
        ScheduledNotification scheduledNotification = scheduledNotificationRepository.findById(id).orElse(null);
        if (scheduledNotification == null || scheduledNotification.getStatus() != ScheduledNotificationStatus.DISPATCHING) {
            // lease에 성공했을 때만 이벤트가 발행되므로 정상 흐름에서는 발생하지 않는다.
            // 방어적으로만 남겨둔다.
            log.warn("Scheduled notification not in DISPATCHING state, skipping: id={}", id);
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (scheduledNotification.getScheduledAt().isBefore(now.minusMinutes(maxDelayMinutes))) {
            txService.markExpired(id);
            log.warn("Scheduled notification expired without sending: id={}, scheduledAt={}, now={}",
                    id, scheduledNotification.getScheduledAt(), now);
            return;
        }

        try {
            AdminNotificationRequest request = objectMapper.readValue(
                    scheduledNotification.getRequestPayload(), AdminNotificationRequest.class);
            AdminNotificationDispatch dispatch = fcmService.prepareAdminNotification(request);
            txService.markSent(id, dispatch.fcmMessageId());

            log.info("Scheduled notification dispatched: id={}, fcmMessageId={}", id, dispatch.fcmMessageId());

            if (dispatch.hasTarget() || dispatch.hasMemberTarget()) {
                fcmService.sendToMembers(dispatch);
            }
        } catch (Exception e) {
            txService.markFailed(id, e.getMessage());
            log.error("Scheduled notification dispatch failed: id={}, message={}", id, e.getMessage(), e);
        }
    }
}
