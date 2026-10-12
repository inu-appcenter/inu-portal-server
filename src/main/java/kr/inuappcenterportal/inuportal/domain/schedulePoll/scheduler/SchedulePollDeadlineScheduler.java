package kr.inuappcenterportal.inuportal.domain.schedulePoll.scheduler;

import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.SchedulePollStatus;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollRepository;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.service.SchedulePollShareService;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.service.SchedulePollTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

// 1분마다 마감 1시간 전 알림을 보내고, 마감 시각이 지난 조율을 정리한다. 요청 경로와 같은 마감 처리 로직을 쓰고,
// 이미 처리된 조율은 건너뛰므로 여러 번 돌거나 요청과 겹쳐도 한 번만 처리된다.
// 이 메서드에는 @Transactional을 붙이지 않는다. 조율마다 SchedulePollTransactionService가 따로 트랜잭션을 연다.
@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulePollDeadlineScheduler {

    private final SchedulePollRepository schedulePollRepository;
    private final SchedulePollTransactionService schedulePollTransactionService;
    private final SchedulePollShareService schedulePollShareService;
    private final Clock clock;

    @Scheduled(fixedDelay = 60_000)
    @SchedulerLock(name = "schedule-poll-deadline", lockAtMostFor = "PT50S", lockAtLeastFor = "PT10S")
    public void closeDuePolls() {
        List<Long> dueIds = schedulePollRepository.findDueIds(SchedulePollStatus.CLOSED, LocalDateTime.now(clock));
        int closed = 0;
        for (Long pollId : dueIds) {
            try {
                if (schedulePollTransactionService.closeIfDue(pollId)) {
                    closed++;
                }
            } catch (Exception e) {
                // 한 조율이 실패해도 나머지는 계속 처리하고, 실패한 조율은 다음 실행에서 다시 시도한다.
                log.error("[SchedulePollDeadlineScheduler] 마감 처리 실패 pollId={}", pollId, e);
            }
        }
        if (closed > 0) {
            log.info("[SchedulePollDeadlineScheduler] 마감 처리 {}건", closed);
        }
    }

    // 마감까지 1시간 이내이고 아직 알림을 보내지 않은 조율마다, 카드가 공유된 방에 한 번씩 메시지를 보낸다.
    @Scheduled(fixedDelay = 60_000)
    @SchedulerLock(name = "schedule-poll-deadline-reminder", lockAtMostFor = "PT50S", lockAtLeastFor = "PT10S")
    public void remindDuePolls() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> dueIds = schedulePollRepository.findReminderDueIds(SchedulePollStatus.CLOSED, now, now.plusHours(1));
        for (Long pollId : dueIds) {
            try {
                schedulePollShareService.sendReminder(schedulePollShareService.prepareReminder(pollId));
            } catch (Exception e) {
                log.error("[SchedulePollDeadlineScheduler] 마감 1시간 전 알림 실패 pollId={}", pollId, e);
            }
        }
    }
}
