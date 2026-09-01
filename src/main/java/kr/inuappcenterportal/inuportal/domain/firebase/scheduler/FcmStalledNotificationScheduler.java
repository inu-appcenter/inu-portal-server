package kr.inuappcenterportal.inuportal.domain.firebase.scheduler;

import jakarta.annotation.PostConstruct;
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
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 커밋 이후 발송 이벤트가 유실된 알림을 보정한다.
 * <p>
 * {@code @TransactionalEventListener(AFTER_COMMIT)}는 커밋 직후 애플리케이션이 종료되면
 * 실행되지 않는다. 이 경우 {@link FcmMessage}가 {@link FcmSendStatus#PENDING}에 남는다.
 * <p>
 * <b>세 가지 안전장치로 #431 사고(레거시 backfill 잔재의 재발송)를 구조적으로 막는다.</b>
 * <ol>
 *     <li>{@code notBefore} 하한 — 이 시각 이전에 생성된 행은 절대 재처리 대상으로
 *     보지 않는다. 배포 시 실행하는 정리 마이그레이션의 CUTOFF와 반드시 같은 값이어야
 *     한다.</li>
 *     <li>{@code maxAgeMinutes} 상한 — 너무 오래 PENDING에 머문 행은 재발송하지 않고
 *     ABANDONED로 종결한다. 알림은 시의성이 본질이므로 뒤늦은 발송은 실패보다 나쁘다.</li>
 *     <li>원자적 lease — 재발행 전에 {@code UPDATE ... WHERE status = PENDING} 조건부
 *     갱신으로 선점한 행만 이벤트를 발행한다. 같은 행이 이미 발송 중(PROCESSING)이면
 *     lease가 실패해 중복 발송을 피한다.</li>
 * </ol>
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

    /** 기본값 false. 명시적으로 켜기 전까지는 아무 것도 하지 않는다. */
    @Value("${fcm.recovery.enabled:false}")
    private boolean recoveryEnabled;

    /**
     * 이 시각 이전에 생성된 fcm_message는 절대 건드리지 않는다. ISO-8601
     * ({@code yyyy-MM-ddTHH:mm:ss})로 설정하며, 레거시 PENDING 정리 마이그레이션의
     * CUTOFF와 같은 값으로 고정해야 한다. recovery.enabled=true인데 비어 있으면
     * 기동을 막는다 (fail-fast) — 값을 잊고 켜면 #431이 그대로 재현되기 때문이다.
     */
    @Value("${fcm.recovery.not-before:}")
    private String notBeforeRaw;

    /** 이 시간(분)보다 오래 같은 상태에 머문 알림을 보정 대상으로 본다. */
    @Value("${fcm.recovery.stalled-after-minutes:10}")
    private long stalledAfterMinutes;

    /** 생성된 지 이만큼(분) 지난 PENDING은 재발송하지 않고 ABANDONED로 종결한다. */
    @Value("${fcm.recovery.max-age-minutes:60}")
    private long maxAgeMinutes;

    /** 한 번의 실행에서 재발행할 최대 건수. 대량 적체 시 부하가 몰리지 않도록 제한한다. */
    @Value("${fcm.recovery.max-per-run:20}")
    private int maxPerRun;

    private LocalDateTime notBefore;

    @PostConstruct
    void validateConfig() {
        if (!recoveryEnabled) {
            return;
        }
        if (notBeforeRaw == null || notBeforeRaw.isBlank()) {
            throw new IllegalStateException(
                    "fcm.recovery.enabled=true인데 fcm.recovery.not-before가 설정되지 않았습니다. " +
                    "레거시 PENDING 잔재가 재발송 대상이 되는 사고(#431)를 막기 위한 필수 하한입니다.");
        }
        try {
            this.notBefore = LocalDateTime.parse(notBeforeRaw);
        } catch (DateTimeParseException e) {
            throw new IllegalStateException(
                    "fcm.recovery.not-before 형식이 올바르지 않습니다 (ISO-8601, 예: 2026-08-30T00:00:00): " + notBeforeRaw, e);
        }
        if (maxAgeMinutes <= stalledAfterMinutes) {
            throw new IllegalStateException(
                    "fcm.recovery.max-age-minutes(" + maxAgeMinutes + ")는 " +
                    "fcm.recovery.stalled-after-minutes(" + stalledAfterMinutes + ")보다 커야 합니다.");
        }
    }

    @Scheduled(fixedDelayString = "${fcm.recovery.interval-ms:300000}")
    @SchedulerLock(
            name = "fcm-stalled-notification-recovery",
            lockAtMostFor = "PT4M",
            lockAtLeastFor = "PT30S"
    )
    public void recoverStalledNotifications() {
        if (!recoveryEnabled) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime stalledBefore = now.minusMinutes(stalledAfterMinutes);
        LocalDateTime maxAgeBefore = now.minusMinutes(maxAgeMinutes);

        abandonExceededMaxAge(maxAgeBefore);
        republishPendingNotifications(stalledBefore);
        finalizeStalledProcessing(stalledBefore);
    }

    /**
     * notBefore 이후 생성됐지만 maxAgeMinutes보다 오래 PENDING에 머문 행을 일괄 ABANDONED로
     * 종결한다. notBefore 이전 행은 이 쿼리의 하한 조건에 걸려 애초에 대상이 되지 않는다.
     */
    @Transactional
    public void abandonExceededMaxAge(LocalDateTime maxAgeBefore) {
        int abandoned = fcmMessageRepository.abandonPendingOlderThan(notBefore, maxAgeBefore);
        if (abandoned > 0) {
            log.warn("Abandoned PENDING notifications past max age: count={}, maxAgeBefore={}", abandoned, maxAgeBefore);
        }
    }

    /**
     * 발송이 시작되지 못한 채 stalledAfterMinutes 이상 정체된 PENDING을 재발행한다.
     * notBefore~maxAge 구간에 있는 행만 후보가 된다 (더 오래된 행은 위에서 이미 종결됐다).
     */
    @Transactional
    public void republishPendingNotifications(LocalDateTime stalledBefore) {
        Pageable pageable = PageRequest.of(0, maxPerRun);
        List<FcmMessage> candidates = fcmMessageRepository.findStalledCandidates(
                FcmSendStatus.PENDING, notBefore, stalledBefore, pageable);

        if (candidates.isEmpty()) {
            return;
        }

        int republished = 0;
        for (FcmMessage fcmMessage : candidates) {
            // 원자적 선점: 이 시점에 이미 발송이 시작됐다면(PROCESSING으로 전이됐다면)
            // lease가 0을 반환하고, 이 행은 건드리지 않는다.
            if (fcmMessageRepository.leasePendingForRecovery(fcmMessage.getId()) != 1) {
                continue;
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
     * 발송이 시작됐지만(PROCESSING) stalledAfterMinutes 이상 끝나지 않은 알림은
     * 재발송하지 않는다. 어디까지 나갔는지 알 수 없어 재발송이 곧 중복 푸시가 되기
     * 때문이다. completeProcessing()은 이미 누적된 sendCount/failureCount를 그대로
     * 최종 상태로 확정할 뿐 리셋하지 않으므로, 부분 성공 집계가 지워지지 않는다 (#431).
     */
    @Transactional
    public void finalizeStalledProcessing(LocalDateTime stalledBefore) {
        Pageable pageable = PageRequest.of(0, maxPerRun);
        List<FcmMessage> stuck = fcmMessageRepository.findStalledCandidates(
                FcmSendStatus.PROCESSING, notBefore, stalledBefore, pageable);

        for (FcmMessage fcmMessage : stuck) {
            log.error("Notification stuck in PROCESSING, finalizing without resend: fcmMessageId={}, stalledSince={}",
                    fcmMessage.getId(), fcmMessage.getModifiedDate());
            fcmMessage.completeProcessing();
        }
    }

    private TrackedNotificationDispatch rebuildDispatch(FcmMessage fcmMessage) {
        List<Long> memberIds = memberFcmMessageRepository.findMemberIdsByFcmMessageId(fcmMessage.getId());
        if (memberIds.isEmpty()) {
            // completeProcessing()은 failureCount==0이면 SUCCESS로 확정한다. 아무에게도
            // 전달할 수 없는 이 케이스에서는 그 휴리스틱을 타지 않고 NO_TARGET을 명시한다.
            log.warn("Stalled notification has no inbox rows, marking as no-target: fcmMessageId={}", fcmMessage.getId());
            fcmMessage.markPending(0);
            return null;
        }

        List<FcmMessageType> types = memberFcmMessageRepository.findDistinctTypesByFcmMessageId(fcmMessage.getId());
        if (types.size() != 1) {
            // type이 둘 이상(또는 0)이면 임의로 하나를 골라 잘못된 라우팅으로 재발송하는
            // 대신 복원을 포기한다. PROCESSING 상태로 남아 다음 tick의 finalize 대상이 된다.
            log.error("Stalled notification has ambiguous type, skipping recovery: fcmMessageId={}, types={}",
                    fcmMessage.getId(), types);
            return null;
        }

        List<FcmToken> fcmTokens = fcmTokenRepository.findFcmTokensByMemberIds(memberIds);
        Map<String, Long> tokenAndMemberId = fcmTokens.stream()
                .collect(java.util.stream.Collectors.toMap(
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
                types.get(0),
                fcmMessage.getTargetId(),
                fcmMessage.getPath()
        );
    }
}
