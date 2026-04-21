package kr.inuappcenterportal.inuportal.domain.firebase.service;

import kr.inuappcenterportal.inuportal.domain.firebase.dto.AdminNotificationDispatch;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FcmAsyncService {

    private final FcmService fcmService;

    @Async("messageExecutor")
    public void sendAsyncKeywordNotice(Map<String, Long> tokenAndMemberId, String title, String body, kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType fcmMessageType) {
        fcmService.sendKeywordNotice(tokenAndMemberId, title, body, fcmMessageType);
    }

    @Async("messageExecutor")
    public void sendAsyncToMembers(AdminNotificationDispatch dispatch) {
        if (!dispatch.hasTarget() && !dispatch.hasMemberTarget()) {
            return;
        }
        fcmService.sendToMembers(dispatch);
    }
}
