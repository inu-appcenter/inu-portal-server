package kr.inuappcenterportal.inuportal.domain.firebase.service;

import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FcmTransactionService {

    private final FcmMessageRepository fcmMessageRepository;

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
