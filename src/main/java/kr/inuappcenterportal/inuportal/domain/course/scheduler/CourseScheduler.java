package kr.inuappcenterportal.inuportal.domain.course.scheduler;

import kr.inuappcenterportal.inuportal.domain.course.service.CourseCrawlerService;
import kr.inuappcenterportal.inuportal.domain.course.service.CourseOfferingSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class CourseScheduler {

    private final CourseCrawlerService courseCrawlerService;
    private final CourseOfferingSyncService courseOfferingSyncService;

    /**
     * 서버 시작 시 강의 기본 정보를 1회 동기화
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncBaseCoursesOnStartup() {
        try {
            log.info("강의 기본 정보 동기화 시작");
            courseCrawlerService.syncBaseCourses();
        } catch (Exception e) {
            log.warn("강의 기본 정보 동기화 실패", e);
        }
    }

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


    /**
     * 매일 오전 3시에 동기화 메서드 실행
     */
    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(
            name = "courseOffering-sync",
            lockAtMostFor = "PT30M",
            lockAtLeastFor = "PT1M"
    )
    public void syncCourseOffering() {
        // 학기마다 주기적으로 바꿔줘야하는 값
        int year = 2026;
        String modDate = "20260101";
        courseOfferingSyncService.syncCourseWithSchoolApi(year, modDate);
    }
}
