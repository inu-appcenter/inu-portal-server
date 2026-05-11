package kr.inuappcenterportal.inuportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing; // 추가
import org.springframework.scheduling.annotation.EnableScheduling; // 추가

@EnableJpaAuditing // 추가
@EnableScheduling // 추가
@SpringBootApplication
public class InuPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(InuPortalApplication.class, args);
    }

}
