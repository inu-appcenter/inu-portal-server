package kr.inuappcenterportal.inuportal.domain.notice.service;

import kr.inuappcenterportal.inuportal.domain.keyword.service.KeywordService;
import kr.inuappcenterportal.inuportal.domain.notice.model.NoticeCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeEventListener {

    private final KeywordService keywordService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNoticeCreatedEvent(NoticeCreatedEvent event) {
        log.info("[학교공지 이벤트] 트랜잭션 커밋 완료, 알림 발송 시작: [{}] {}", 
                event.getNotice().getCategory(), event.getNotice().getTitle());
        try {
            keywordService.noticeNotifyMatchedUsers(event.getNotice());
        } catch (Exception e) {
            log.error("[학교공지 이벤트] 알림 발송 중 에러 발생: {}", e.getMessage(), e);
        }
    }
}
