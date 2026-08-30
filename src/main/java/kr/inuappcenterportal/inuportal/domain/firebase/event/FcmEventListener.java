package kr.inuappcenterportal.inuportal.domain.firebase.event;

import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 알림 저장 트랜잭션이 커밋된 뒤에 실제 FCM 발송을 수행한다.
 * <p>
 * 롤백된 트랜잭션의 알림이 나가지 않도록 {@link TransactionPhase#AFTER_COMMIT}을 쓰고,
 * 외부 API 응답 시간이 메인 로직에 영향을 주지 않도록 {@code @Async}로 처리한다.
 * <p>
 * {@code @EventListener}를 함께 붙이지 않는다. 두 애노테이션을 병용하면 트랜잭션 밖에서
 * 발행됐을 때 중복 발화할 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FcmEventListener {

    private final FcmService fcmService;

    @Async("messageExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTrackedNotification(TrackedNotificationDispatchEvent event) {
        // AFTER_COMMIT 리스너의 예외는 이미 커밋된 트랜잭션을 롤백하지 않는다.
        // 발송 실패는 FcmSendStatus로만 기록하고, 미처리 건은 스케줄러가 보정한다.
        try {
            fcmService.dispatchTrackedNotification(event.dispatch());
        } catch (Exception e) {
            log.error("Tracked notification dispatch failed: fcmMessageId={}, message={}",
                    event.dispatch() == null ? null : event.dispatch().fcmMessageId(), e.getMessage(), e);
        }
    }
}
