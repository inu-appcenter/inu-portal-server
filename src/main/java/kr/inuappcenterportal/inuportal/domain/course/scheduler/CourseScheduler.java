package kr.inuappcenterportal.inuportal.domain.course.scheduler;

import kr.inuappcenterportal.inuportal.domain.course.service.CourseCrawlerService;
import kr.inuappcenterportal.inuportal.domain.course.service.CourseOfferingSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class CourseScheduler {

    private final CourseCrawlerService courseCrawlerService;
    private final CourseOfferingSyncService courseOfferingSyncService;


    /**
     * 매월 1일 오전 4시에 동기화 메서드 실행
     * 크롤링으로 강의 기본정보 동기화
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
     * 학교API로 개설 강의 동기화
     */
    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(
            name = "courseOffering-sync",
            lockAtMostFor = "PT30M",
            lockAtLeastFor = "PT1M"
    )
    public void syncCourseOffering() {
        int year = LocalDate.now().getYear();
        String modDate = year + "0101";
        courseOfferingSyncService.syncCourseWithSchoolApi(year, modDate);
    }
}
