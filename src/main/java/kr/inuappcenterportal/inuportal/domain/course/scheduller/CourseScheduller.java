package kr.inuappcenterportal.inuportal.domain.course.scheduller;

import kr.inuappcenterportal.inuportal.domain.course.service.CourseCrawlerService;
import kr.inuappcenterportal.inuportal.domain.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseScheduller {

    private final CourseService courseService;
    private final CourseCrawlerService courseCrawlerService;

    /**
     * 매월 1일 오전 4시에 동기화 메서드 실행
     */
    @Scheduled(cron = "0 0 4 1 * *")
    public void syncBaseCourses() {
        courseCrawlerService.syncBaseCourses();
    }
}
