package kr.inuappcenterportal.inuportal.domain.firebase.service;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.model.MemberFcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FcmTransactionService {

    private final FcmMessageRepository fcmMessageRepository;
    private final MemberFcmMessageRepository memberFcmMessageRepository;

    /**
     * 회원 한 명에게 가는 알림의 발송 이력({@code fcm_message})과 알림함 행({@code member_fcm_message})을 만든다.
     * 발송(외부 HTTP)보다 먼저 짧은 트랜잭션으로 끝내, 발송하는 동안 DB 커넥션을 붙잡지 않게 한다.
     *
     * @return 만든 FcmMessage ID. 발송 결과는 {@link #updateFinalStatus}로 반영한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long createMemberNotification(String title, String body, Long memberId, FcmMessageType type) {
        FcmMessage fcmMessage = fcmMessageRepository.save(FcmMessage.builder()
                .title(title)
                .body(body)
                .targetId(null)
                .isAdminMessage(false)
                .build());
        memberFcmMessageRepository.save(MemberFcmMessage.of(fcmMessage.getId(), memberId, type));
        return fcmMessage.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusToProcessing(Long fcmMessageId) {
        fcmMessageRepository.findById(fcmMessageId).ifPresent(FcmMessage::markProcessing);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateIncrementalResult(Long fcmMessageId, int batchSuccess, int batchFailure) {
        fcmMessageRepository.findById(fcmMessageId).ifPresent(message ->
                message.incrementDeliveryResult(batchSuccess, batchFailure));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateFinalStatus(Long fcmMessageId, int totalSuccess, int totalFailure) {
        fcmMessageRepository.findById(fcmMessageId).ifPresent(message -> {
            message.updateDeliveryResult(totalSuccess, totalFailure);
            message.completeProcessing();
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(Long fcmMessageId, int targetCount) {
        fcmMessageRepository.findById(fcmMessageId).ifPresent(message -> message.markFailed(targetCount));
    }

    /**
     * 재시도 결과를 확정한다. 재시도는 실패자에게만 나가므로 이전 성공분({@code previousSendCount})은
     * 그대로 살리고 실패 건수만 이번 결과로 대체한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyRetryResult(Long fcmMessageId, int previousSendCount, int retrySuccess, int retryFailure) {
        fcmMessageRepository.findById(fcmMessageId).ifPresent(message ->
                message.applyRetryResult(previousSendCount, retrySuccess, retryFailure));
    }
}
