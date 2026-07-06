package kr.inuappcenterportal.inuportal.domain.course.scheduler;

import kr.inuappcenterportal.inuportal.domain.course.service.CourseCrawlerService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseScheduler {

    private final CourseCrawlerService courseCrawlerService;

    /**
     * 매월 1일 오전 4시에 동기화 메서드 실행
     */
    @Scheduled(cron = "0 0 4 1 * *")
    @SchedulerLock(
            name = "course-base-sync",
            lockAtMostFor = "PT30M",
            lockAtLeastFor = "PT1M"
    )
    public void syncBaseCourses() {
        courseCrawlerService.syncBaseCourses();
    }
}
