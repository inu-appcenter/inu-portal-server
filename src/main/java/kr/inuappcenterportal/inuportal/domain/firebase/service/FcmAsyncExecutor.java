package kr.inuappcenterportal.inuportal.domain.firebase.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
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
    private final FirebaseMessaging firebaseMessaging;
    private final List<String> failedTokensList = Collections.synchronizedList(new ArrayList<>());

    @Async("sendExecutor")
    public CompletableFuture<Void> sendMessage(List<String> tokens, String body, String title) {
        try {
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                if (!responses.get(i).isSuccessful()) {
                    failedTokensList.add(tokens.get(i));
                }
            }
        } catch (FirebaseMessagingException e) {
            failedTokensList.addAll(tokens);
            log.warn("FCM batch send failed: {}", e.getMessage());
        } catch (Exception e) {
            failedTokensList.addAll(tokens);
            log.error("FCM batch send failed unexpectedly: batchSize={}, message={}",
                    tokens.size(), e.getMessage(), e);
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
