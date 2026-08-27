package kr.inuappcenterportal.inuportal.domain.firebase.scheduler;

import kr.inuappcenterportal.inuportal.domain.firebase.dto.TrackedNotificationDispatch;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmSendStatus;
import kr.inuappcenterportal.inuportal.domain.firebase.event.TrackedNotificationDispatchEvent;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 커밋 이후 발송 이벤트가 유실된 알림을 보정한다.
 * <p>
 * {@code @TransactionalEventListener(AFTER_COMMIT)}는 커밋 직후 애플리케이션이 종료되면
 * 실행되지 않는다. 이 경우 {@link FcmMessage}가 {@link FcmSendStatus#PENDING}에 남는다.
 * <p>
 * <b>PENDING만 재발송한다.</b> PENDING은 {@code dispatchTrackedNotification}이 시작조차
 * 하지 못했다는 뜻이므로 재발송해도 중복이 생기지 않는다. 반면 PROCESSING은 발송이 이미
 * 시작된 상태라 어디까지 나갔는지 알 수 없으므로, 재발송하지 않고 실패로 확정만 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FcmStalledNotificationScheduler {

    private static final long UNLINKED_MEMBER_ID = -1L;

    private final FcmMessageRepository fcmMessageRepository;
    private final MemberFcmMessageRepository memberFcmMessageRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 이 시간(분)보다 오래 같은 상태에 머문 알림을 보정 대상으로 본다. */
    @Value("${fcm.recovery.stalled-after-minutes:10}")
    private long stalledAfterMinutes;

    /** 한 번의 실행에서 처리할 최대 건수. 대량 적체 시 부하가 몰리지 않도록 제한한다. */
    @Value("${fcm.recovery.max-per-run:20}")
    private int maxPerRun;

    @Scheduled(fixedDelayString = "${fcm.recovery.interval-ms:300000}")
    @Transactional
    public void recoverStalledNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(stalledAfterMinutes);

        republishPendingNotifications(threshold);
        markStalledProcessingAsFailed(threshold);
    }

    /**
     * 발송이 시작되지 못한 알림을 다시 발행한다.
     * 이 메서드는 트랜잭션 안에서 실행되므로, 커밋 이후 기존 리스너가 동일한 경로로 발송한다.
     */
    private void republishPendingNotifications(LocalDateTime threshold) {
        List<FcmMessage> pending = fcmMessageRepository
                .findAllBySendStatusAndModifiedDateBefore(FcmSendStatus.PENDING, threshold);

        if (pending.isEmpty()) {
            return;
        }

        int republished = 0;
        for (FcmMessage fcmMessage : pending) {
            if (republished >= maxPerRun) {
                log.warn("Stalled notification recovery hit per-run limit: limit={}, remaining={}",
                        maxPerRun, pending.size() - republished);
                break;
            }

            TrackedNotificationDispatch dispatch = rebuildDispatch(fcmMessage);
            if (dispatch == null) {
                continue;
            }

            eventPublisher.publishEvent(new TrackedNotificationDispatchEvent(dispatch));
            republished++;

            log.warn("Republished stalled notification: fcmMessageId={}, targets={}, stalledSince={}",
                    fcmMessage.getId(), dispatch.tokenAndMemberId().size(), fcmMessage.getModifiedDate());
        }

        if (republished > 0) {
            log.warn("Stalled notification recovery finished: republished={}", republished);
        }
    }

    /**
     * 발송이 시작됐지만 끝나지 않은 알림은 재발송하지 않는다.
     * 어디까지 나갔는지 알 수 없어 재발송이 곧 중복 푸시가 되기 때문이다.
     */
    private void markStalledProcessingAsFailed(LocalDateTime threshold) {
        List<FcmMessage> processing = fcmMessageRepository
                .findAllBySendStatusAndModifiedDateBefore(FcmSendStatus.PROCESSING, threshold);

        for (FcmMessage fcmMessage : processing) {
            log.error("Notification stuck in PROCESSING, marking as failed without resend: fcmMessageId={}, stalledSince={}",
                    fcmMessage.getId(), fcmMessage.getModifiedDate());
            fcmMessage.markFailed(fcmMessage.getTargetCount());
        }
    }

    private TrackedNotificationDispatch rebuildDispatch(FcmMessage fcmMessage) {
        List<Long> memberIds = memberFcmMessageRepository.findMemberIdsByFcmMessageId(fcmMessage.getId());
        if (memberIds.isEmpty()) {
            log.warn("Stalled notification has no inbox rows, skipping: fcmMessageId={}", fcmMessage.getId());
            return null;
        }

        List<FcmToken> fcmTokens = fcmTokenRepository.findFcmTokensByMemberIds(memberIds);
        if (fcmTokens.isEmpty()) {
            log.warn("Stalled notification has no push targets, skipping: fcmMessageId={}", fcmMessage.getId());
            return null;
        }

        Map<String, Long> tokenAndMemberId = fcmTokens.stream()
                .collect(Collectors.toMap(
                        FcmToken::getToken,
                        token -> token.getMemberId() == null ? UNLINKED_MEMBER_ID : token.getMemberId(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        return new TrackedNotificationDispatch(
                fcmMessage.getId(),
                tokenAndMemberId,
                fcmMessage.getTitle(),
                fcmMessage.getBody(),
                resolveType(fcmMessage.getId()),
                fcmMessage.getTargetId(),
                fcmMessage.getPath()
        );
    }

    private FcmMessageType resolveType(Long fcmMessageId) {
        return memberFcmMessageRepository.findTypesByFcmMessageId(fcmMessageId).stream()
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
