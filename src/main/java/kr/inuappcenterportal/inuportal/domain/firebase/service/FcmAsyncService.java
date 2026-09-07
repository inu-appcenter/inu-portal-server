package kr.inuappcenterportal.inuportal.domain.firebase.service;

import kr.inuappcenterportal.inuportal.domain.firebase.dto.AdminNotificationDispatch;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FcmAsyncService {

    private final FcmService fcmService;

    @Async("messageExecutor")
    public void sendAsyncKeywordNotice(Map<String, Long> tokenAndMemberId, String title, String body, kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType fcmMessageType) {
        Long fcmMessageId = fcmService.prepareKeywordNotice(tokenAndMemberId, title, body, fcmMessageType, null);
        fcmService.dispatchKeywordNotice(fcmMessageId, tokenAndMemberId, title, body, fcmMessageType, null);
    }

    @Async("messageExecutor")
    public void sendAsyncKeywordNotice(Map<String, Long> tokenAndMemberId, String title, String body, kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType fcmMessageType, Long targetId) {
        Long fcmMessageId = fcmService.prepareKeywordNotice(tokenAndMemberId, title, body, fcmMessageType, targetId);
        fcmService.dispatchKeywordNotice(fcmMessageId, tokenAndMemberId, title, body, fcmMessageType, targetId);
    }

    @Async("messageExecutor")
    public void sendAsyncKeywordNotice(Map<String, Long> tokenAndMemberId, String title, String body, kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType fcmMessageType, Long targetId, String path) {
        Long fcmMessageId = fcmService.prepareKeywordNotice(tokenAndMemberId, title, body, fcmMessageType, targetId, path);
        fcmService.dispatchKeywordNotice(fcmMessageId, tokenAndMemberId, title, body, fcmMessageType, targetId, path);
    }

    @Async("messageExecutor")
    public void sendAsyncToMembers(AdminNotificationDispatch dispatch) {
        fcmService.sendToMembers(dispatch);
    }

    /**
     * 실패자에게만 다시 보낸다. 알림함 행은 최초 발송 때 이미 만들어져 있으므로 새로 만들지 않는다.
     * 선점(lease)은 호출 전에 끝나 있어야 한다 — 여기서 하면 이미 비동기라 연타를 막지 못한다.
     */
    @Async("messageExecutor")
    public void retryAsync(Long fcmMessageId, Map<String, Long> tokenAndMemberId, String title, String body,
                           kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType type,
                           Long targetId, String path, int previousSendCount) {
        fcmService.retryFailedTargets(fcmMessageId, tokenAndMemberId, title, body, type, targetId, path, previousSendCount);
    }

    // prepareTrackedNotification이 저장 트랜잭션 커밋 이후 발송을 이벤트로 트리거한다.
    // 여기서 dispatchTrackedNotification을 또 호출하면 중복 발송된다.

    @Async("messageExecutor")
    public void sendAsyncTrackedNotification(List<Long> memberIds, String title, String body, kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType type) {
        fcmService.prepareTrackedNotification(memberIds, title, body, type, null);
    }

    @Async("messageExecutor")
    public void sendAsyncTrackedNotification(List<Long> memberIds, String title, String body, kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType type, Long targetId) {
        fcmService.prepareTrackedNotification(memberIds, title, body, type, targetId);
    }

    @Async("messageExecutor")
    public void sendAsyncTrackedNotification(List<Long> memberIds, String title, String body, kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType type, Long targetId, String path) {
        fcmService.prepareTrackedNotification(memberIds, title, body, type, targetId, path);
    }

    @Async("messageExecutor")
    public void sendAsyncUntrackedNotification(List<Long> memberIds, String title, String body) {
        fcmService.sendUntrackedNotification(memberIds, title, body);
    }

    @Async("messageExecutor")
    public void sendAsyncChatNotification(List<Long> memberIds, String title, String body, Long chatRoomId, boolean isMuted) {
        fcmService.sendChatNotification(memberIds, title, body, chatRoomId, isMuted);
    }
}
