package kr.inuappcenterportal.inuportal;

import kr.inuappcenterportal.inuportal.domain.course.crawler.excel.CourseOverviewProperties;
import kr.inuappcenterportal.inuportal.domain.course.dto.api.SchoolApiProperties;
import kr.inuappcenterportal.inuportal.global.config.VllmProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing // 추가
@EnableScheduling // 추가
@SpringBootApplication
@EnableConfigurationProperties({SchoolApiProperties.class, CourseOverviewProperties.class, VllmProperties.class})
public class InuPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(InuPortalApplication.class, args);
    }

}
