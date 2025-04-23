package kr.inuappcenterportal.inuportal.domain.firebase.service;


import com.google.firebase.messaging.*;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.model.Message;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MessageRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcmService {
    private final FcmTokenRepository fcmTokenRepository;
    private final MessageRepository messageRepository;
    private final FcmAsyncExecutor fcmAsyncExecutor;
    private final Set<String> failedTokensSet = ConcurrentHashMap.newKeySet();


    @Transactional
    public void saveToken(String token, Long memberId){
        if(fcmTokenRepository.existsByToken(token)){
            FcmToken fcmToken =fcmTokenRepository.findByToken(token).orElseThrow(()->new MyException(MyErrorCode.TOKEN_NOT_FOUND));
            fcmToken.updateTimeNow();
        }
        else {
            FcmToken fcmToken = FcmToken.builder().token(token).memberId(memberId).build();
            fcmTokenRepository.save(fcmToken);
        }
    }

    @Transactional
    public void deleteToken(Long memberId){
        FcmToken fcmToken =fcmTokenRepository.findByMemberId(memberId).orElseThrow(()->new MyException(MyErrorCode.TOKEN_NOT_FOUND));
        fcmToken.clearMemberId();
    }

    @Transactional
    @Async("messageExecutor")
    public void sendToAdmin(String title, String body){
        List<String> target = fcmTokenRepository.findAllAdminTokens();
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(target)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();
        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("관리자 알림 전송 성공");
            List<String> failToken = new ArrayList<>();
            List<SendResponse> responses = response.getResponses();
            for(int i = 0 ; i < responses.size() ; i++){
                if(!responses.get(i).isSuccessful()){
                    failToken.add(target.get(i));
                }
            }
            if(!failToken.isEmpty()){
                fcmTokenRepository.deleteByTokenIn(failToken);
            }
            messageRepository.save(Message.builder().title(title).body(body).build());
        } catch (FirebaseMessagingException e) {
            log.info("메시지 전송 실패 : {}",e.getMessage());
        }
    }

    @Transactional
    @Async("messageExecutor")
    public void sendToAll(String title, String body) {
        List<String> target = fcmTokenRepository.findAllTokens();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < target.size(); i += 500) {
            List<String> tokens = target.subList(i, Math.min(i + 500, target.size()));
            futures.add(fcmAsyncExecutor.sendMessage(tokens, body, title));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        List<String> failedTokens = fcmAsyncExecutor.getFailedTokensList();
        if(!failedTokensSet.isEmpty()){
            fcmTokenRepository.deleteByTokenIn(new ArrayList<>(failedTokens));
            fcmAsyncExecutor.clearFailedTokenSet();
        }
        messageRepository.save(Message.builder().title(title).body(body).build());
    }

    @Transactional
    //@Async("messageExecutor")
    public void noticeAll(String title){
        com.google.firebase.messaging.Message message = com.google.firebase.messaging.Message.builder()
                .setTopic("notice")
                .setNotification(
                        Notification.builder()
                                .setTitle("인천대학교 총학생회")
                                .setBody(title)
                                .build()
                )
                .build();
        try {
            String response = FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            log.warn("전체 공지 실패 : {}",e.getMessage());
        }
        messageRepository.save(Message.builder().title("인천대학교 총학생회").body(title).build());
    }
}
