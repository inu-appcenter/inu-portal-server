package kr.inuappcenterportal.inuportal.domain.firebase.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import kr.inuappcenterportal.inuportal.global.metric.FcmMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmAsyncExecutor {
    private final FcmDispatchGate fcmDispatchGate;
    private final FcmMetrics fcmMetrics;
    private final List<String> failedTokensList = Collections.synchronizedList(new ArrayList<>());

    @Async("sendExecutor")
    public CompletableFuture<Void> sendMessage(List<String> tokens, String body, String title) {
        long startNanos = System.nanoTime();
        int batchSuccess = 0;
        int batchFailure = 0;
        try {
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            // 게이트를 거치지 않고 firebaseMessaging을 직접 호출하면 전역 팬아웃 상한이 깨진다.
            // 이 메서드는 sendExecutor(코어 8/최대 16)에서 병렬 실행되므로,
            // 여기서 상한이 풀리면 청크 크기 × 스레드 수만큼 커넥션이 동시에 열린다.
            BatchResponse response = fcmDispatchGate.send(message);
            batchSuccess = response.getSuccessCount();
            batchFailure = response.getFailureCount();
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                if (!responses.get(i).isSuccessful()) {
                    failedTokensList.add(tokens.get(i));
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            batchFailure = tokens.size();
            failedTokensList.addAll(tokens);
            log.warn("FCM batch send interrupted: batchSize={}", tokens.size());
        } catch (FirebaseMessagingException e) {
            batchFailure = tokens.size();
            failedTokensList.addAll(tokens);
            log.warn("FCM batch send failed: {}", e.getMessage());
        } catch (Exception e) {
            batchFailure = tokens.size();
            failedTokensList.addAll(tokens);
            log.error("FCM batch send failed unexpectedly: batchSize={}, message={}",
                    tokens.size(), e.getMessage(), e);
        } finally {
            fcmMetrics.recordBatch("BROADCAST", tokens.size(), batchSuccess, batchFailure, System.nanoTime() - startNanos);
        }

        return CompletableFuture.completedFuture(null);
    }

    public List<String> getFailedTokensList() {
        synchronized (failedTokensList) {
            return new ArrayList<>(failedTokensList);
        }
    }

    public void clearFailedTokens() {
        failedTokensList.clear();
    }
}
