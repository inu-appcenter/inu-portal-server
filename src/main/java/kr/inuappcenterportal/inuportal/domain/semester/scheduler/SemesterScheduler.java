package kr.inuappcenterportal.inuportal.domain.semester.scheduler;

import kr.inuappcenterportal.inuportal.domain.semester.service.SemesterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SemesterScheduler {

    private final SemesterService semesterService;

    @EventListener(ApplicationReadyEvent.class)
    public void syncSemesterOnStartup() {
        try {
            log.info("학기 동기화 시작");
            semesterService.syncSemestersByYear();
        } catch (Exception e) {
            log.warn("학기 동기화 실패", e);
        }
    }

    /**
     * 매일 오전 3시에 학기 상태 자동 업데이트
     */
    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(
            name = "semester-status-update",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT1M"
    )
    public void updateSemesterStatuses() {
        semesterService.updateSemesterStatus();
    }

    /**
     * 매월 1일 새벽 5시에 학기 자동 동기화
     */
    @Scheduled(cron = "0 0 5 1 * *")
    @SchedulerLock(
            name = "semester-sync",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT1M"
    )
    public void syncSemesters() {
        semesterService.syncSemestersByYear();
    }
}

