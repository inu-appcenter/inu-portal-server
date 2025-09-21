package kr.inuappcenterportal.inuportal.domain.firebase.service;

import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.AdminNotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FcmAsyncService {

    private final FcmService fcmService;

    @Async("messageExecutor")
    public void sendAsyncKeywordNotice(Map<String, Long> tokenAndMemberId, String title, String body) {
        fcmService.sendKeywordNotice(tokenAndMemberId, title, body);
    }

    @Async("messageExecutor")
    public void sendAsyncToMembers(AdminNotificationRequest request) {
        fcmService.sendToMembers(request);
    }
}
