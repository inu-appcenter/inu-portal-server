package kr.inuappcenterportal.inuportal.domain.firebase.service;


import com.google.firebase.messaging.*;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.AdminNotificationRequest;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.res.NotificationResponse;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.model.MemberFcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;
    private final FcmMessageRepository fcmMessageRepository;
    private final MemberFcmMessageRepository memberFcmMessageRepository;
    private final FcmAsyncExecutor fcmAsyncExecutor;
    private final Set<String> failedTokensSet = ConcurrentHashMap.newKeySet();

    @Scheduled(cron = "0 0 0 * * *")
    public void cleanExpiredTokens() {
        fcmTokenRepository.deleteOldTokens();
        log.info("로그아웃 기기 토큰 삭제");
    }

    @Transactional
    public void saveToken(String token, Long memberId){
        FcmToken fcmToken = fcmTokenRepository.findByToken(token)
                .orElse(FcmToken.builder().token(token).memberId(memberId).build());

        fcmToken.updateMemberId(memberId);
        fcmToken.updateTimeNow();

        if (fcmToken.getId() == null){
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

            handleFailedTokens(response, target);

            fcmMessageRepository.save(FcmMessage.builder().title(title).body(body).build());
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
        fcmMessageRepository.save(FcmMessage.builder().title(title).body(body).build());
    }

    @Transactional
    @Async("messageExecutor")
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
        fcmMessageRepository.save(FcmMessage.builder().title("인천대학교 총학생회").body(title).build());
    }

    @Transactional
    @Async("messageExecutor")
    public void sendKeywordNotice(Map<String, Long> tokenAndMemberId, String title, String body) {
        if (tokenAndMemberId.isEmpty()) return;

        List<String> tokens = new ArrayList<>(tokenAndMemberId.keySet());

        sendFcmMessageToMembers(tokenAndMemberId, tokens, title, body, FcmMessageType.DEPARTMENT);
    }

    @Transactional
    @Async("messageExecutor")
    public void sendToMembers(AdminNotificationRequest request) {
        List<FcmToken> fcmTokens;

        if (request.memberIds() == null || request.memberIds().isEmpty()) {
            fcmTokens = fcmTokenRepository.findAll();
        } else {
            fcmTokens = fcmTokenRepository.findFcmTokensByMemberIds(request.memberIds());
        }
        if (fcmTokens.isEmpty()) return;

        Map<String, Long> tokenAndMemberId = fcmTokens.stream()
                .collect(Collectors.toMap(
                        FcmToken::getToken, t -> t.getMemberId() != null ? t.getMemberId() : -1L,
                        (existing, replacement) -> existing));

        List<String> tokens = new ArrayList<>(tokenAndMemberId.keySet());

        sendFcmMessageToMembers(tokenAndMemberId, tokens, request.title(), request.content(), FcmMessageType.GENERAL);
    }

    @Transactional(readOnly = true)
    public ListResponseDto<NotificationResponse> findNotifications(Member member, int page) {
        Pageable pageable = PageRequest.of(page>0?--page:page, 10, Sort.by(Sort.Direction.DESC, "createDate"));

        Page<MemberFcmMessage> messages = memberFcmMessageRepository.findAllByMemberId(member.getId(), pageable);

        List<NotificationResponse> notificationResponses = messages.stream().map(message -> {
            FcmMessage fcmMessage = fcmMessageRepository.findById(message.getFcmMessageId())
                    .orElseThrow(() -> new MyException(MyErrorCode.MESSAGE_NOT_FOUND));
            return NotificationResponse.from(message, fcmMessage);
        }).toList();

        return ListResponseDto.of(messages.getTotalPages(), messages.getTotalElements(), notificationResponses);
    }

    private MulticastMessage createMulticastMessage(List<String> tokens, String title, String body) {
        return MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();
    }

    private void handleFailedTokens(BatchResponse response, List<String> target) {
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
    }

    private void addMemberFcmMessageList(SendResponse sendResponse, String token,
                                         Long memberId, List<MemberFcmMessage> memberFcmMessageList,
                                         FcmMessage fcmMessage, FcmMessageType fcmMessageType) {
        if (sendResponse.isSuccessful()) {
            if (memberId != null && memberId != -1L) {
                memberFcmMessageList.add(MemberFcmMessage.of(fcmMessage.getId(), memberId, fcmMessageType));
            } else {
                log.info("Member FCM 저장 생략 (memberId = null): token={}", token);
            }
        } else {
            FirebaseMessagingException exception = sendResponse.getException();
            String errorMsg = exception != null ? exception.getMessage() : "알 수 없는 오류 (exception=null)";
            log.warn("FCM 전송 실패: token={}, error={}", token, errorMsg);
        }
    }

    private void saveMemberFcmMessage(List<MemberFcmMessage> memberFcmMessageList, int i, int batchSize) {
        List<MemberFcmMessage> batch = memberFcmMessageList.subList(i, Math.min(i + batchSize, memberFcmMessageList.size()));

        Map<Long, MemberFcmMessage> uniqueMemberFcmMessages = batch.stream()
                        .collect(Collectors.toMap(
                                MemberFcmMessage::getMemberId,
                                m -> m,
                                (existing, replacement) -> existing
                        ));

        memberFcmMessageRepository.saveAll(uniqueMemberFcmMessages.values());
        memberFcmMessageRepository.flush();
    }

    private void sendFcmMessageToMembers(Map<String, Long> tokenAndMemberId, List<String> tokens, String title, String body, FcmMessageType fcmMessageType) {
        int batchSize = 500;

        FcmMessage fcmMessage = fcmMessageRepository.save(FcmMessage.builder().title(title).body(body).build());

        List<MemberFcmMessage> memberFcmMessageList = new ArrayList<>();

        try {
            for (int i = 0; i < tokens.size(); i += batchSize) {
                // Batch size 만큼 분리해 처리
                List<String> subTokens = tokens.subList(i, Math.min(i + batchSize, tokens.size()));

                MulticastMessage message = createMulticastMessage(subTokens, title, body);
                BatchResponse batchResponse;
                try {
                    batchResponse = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                } catch (FirebaseMessagingException e) {
                    log.error("FCM 메시지 전송 실패: {}", e.getMessage());
                    continue;
                }

                for (int j = 0; j < batchResponse.getResponses().size(); j++) {
                    String token = subTokens.get(j);
                    Long memberId = tokenAndMemberId.get(token);

                    try {
                        // 전송된 알림들 List화
                        addMemberFcmMessageList(batchResponse.getResponses().get(j), token, memberId,
                                memberFcmMessageList, fcmMessage, fcmMessageType);
                    } catch (Exception e) {
                        log.error("개별 FCM 전송 처리 중 오류: token={}, error={}", token, e.getMessage(), e);
                    }
                }

                handleFailedTokens(batchResponse, subTokens);
            }

            // 전송된 알림들 저장
            for (int i = 0; i < memberFcmMessageList.size(); i += batchSize) {
                saveMemberFcmMessage(memberFcmMessageList, i, batchSize);
            }

            log.info("회원 대상 알림 전송 성공, {}건", memberFcmMessageList.size());
        } catch (Exception e) {
            log.error("회원 대상 알림 전송 실패 : {}", e.getMessage());
        }
    }
}
