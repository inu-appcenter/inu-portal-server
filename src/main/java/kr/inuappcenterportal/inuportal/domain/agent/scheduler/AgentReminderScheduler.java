package kr.inuappcenterportal.inuportal.domain.agent.scheduler;

import kr.inuappcenterportal.inuportal.domain.agent.service.AgentReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentReminderScheduler {

    private final AgentReminderService agentReminderService;

    /**
     * 매 분 0초마다 실행되어 현재 시각에 발송 예약된 맞춤 알림들을 발송합니다.
     */
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void processDueReminders() {
        try {
            agentReminderService.dispatchDueReminders();
        } catch (Exception e) {
            log.error("[AgentReminderScheduler] 알림 스케줄러 실행 중 오류: {}", e.getMessage(), e);
        }
    }
}
