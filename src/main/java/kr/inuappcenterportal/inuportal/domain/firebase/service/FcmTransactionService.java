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
}
