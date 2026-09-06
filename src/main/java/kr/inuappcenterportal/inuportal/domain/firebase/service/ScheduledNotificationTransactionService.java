package kr.inuappcenterportal.inuportal.domain.firebase.service;

import kr.inuappcenterportal.inuportal.domain.firebase.model.ScheduledNotification;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.ScheduledNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 상태 전이를 별도 빈의 {@code REQUIRES_NEW} 트랜잭션으로 분리한다 (FcmTransactionService와
 * 동일한 목적). 스케줄러가 자기 자신의 메서드를 호출하며 프록시를 우회해 트랜잭션이 걸리지
 * 않는 사고를 구조적으로 막기 위함이다.
 */
@Service
@RequiredArgsConstructor
public class ScheduledNotificationTransactionService {

    private final ScheduledNotificationRepository scheduledNotificationRepository;

    /**
     * @param staleBefore 이 시각보다 이전에 마지막으로 선점된 DISPATCHING 행도 재선점
     *                    대상으로 본다 (lease가 커밋된 뒤 리스너가 끝까지 못 간 경우의 복구).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean lease(Long id, LocalDateTime now, LocalDateTime staleBefore) {
        return scheduledNotificationRepository.leaseForDispatch(id, now, staleBefore) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(Long id, Long fcmMessageId) {
        scheduledNotificationRepository.findById(id).ifPresent(s -> s.markSent(fcmMessageId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long id, String reason) {
        scheduledNotificationRepository.findById(id).ifPresent(s -> s.markFailed(reason));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markExpired(Long id) {
        scheduledNotificationRepository.findById(id).ifPresent(ScheduledNotification::markExpired);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean cancelIfScheduled(Long id) {
        return scheduledNotificationRepository.cancelIfScheduled(id) == 1;
    }
}
