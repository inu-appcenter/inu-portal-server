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
        Long fcmMessageId = fcmService.prepareKeywordNotice(tokenAndMemberId, title, body, fcmMessageType);
        fcmService.dispatchKeywordNotice(fcmMessageId, tokenAndMemberId, title, body, fcmMessageType, null);
    }

    @Async("messageExecutor")
    public void sendAsyncKeywordNotice(Map<String, Long> tokenAndMemberId, String title, String body, kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType fcmMessageType, Long targetId) {
        Long fcmMessageId = fcmService.prepareKeywordNotice(tokenAndMemberId, title, body, fcmMessageType);
        fcmService.dispatchKeywordNotice(fcmMessageId, tokenAndMemberId, title, body, fcmMessageType, targetId);
    }

    @Async("messageExecutor")
    public void sendAsyncToMembers(AdminNotificationDispatch dispatch) {
        fcmService.sendToMembers(dispatch);
    }

    @Async("messageExecutor")
    public void sendAsyncTrackedNotification(List<Long> memberIds, String title, String body, kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType type) {
        FcmService.TrackedNotificationDispatch dispatch = fcmService.prepareTrackedNotification(memberIds, title, body, type, null);
        fcmService.dispatchTrackedNotification(dispatch);
    }

    @Async("messageExecutor")
    public void sendAsyncTrackedNotification(List<Long> memberIds, String title, String body, kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType type, Long targetId) {
        FcmService.TrackedNotificationDispatch dispatch = fcmService.prepareTrackedNotification(memberIds, title, body, type, targetId);
        fcmService.dispatchTrackedNotification(dispatch);
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
