package kr.inuappcenterportal.inuportal.domain.firebase.service;

import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationViewService {

    // 알림함 자동 읽음 임계값. hasUnreadNotification과 반드시 같은 값을 써야 배지와 목록이 어긋나지 않음
    public static final int AUTO_READ_VIEW_THRESHOLD = 2;

    private final MemberFcmMessageRepository memberFcmMessageRepository;

    // 같은 회원의 작업이 동시에 돌며 같은 행을 두고 락 경합하는 것을 막는다(인스턴스 단위).
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    @Async("notificationViewExecutor")
    @Transactional
    public void recordVisit(Long memberId) {
        // add가 false면 같은 회원의 작업이 이미 진행 중이므로 건너뛴다.
        if (memberId == null || !inFlight.add(memberId)) {
            return;
        }

        try {
            memberFcmMessageRepository.markAsReadByViewCount(memberId, AUTO_READ_VIEW_THRESHOLD, LocalDateTime.now());
            memberFcmMessageRepository.incrementViewCountForAllUnread(memberId);
        } catch (Exception e) {
            // 응답은 이미 나갔고 조회수 한 번 누락은 무해하므로 재시도하지 않는다.
            log.warn("알림 조회수 반영 실패 memberId={}", memberId, e);
        } finally {
            inFlight.remove(memberId);
        }
    }
}
