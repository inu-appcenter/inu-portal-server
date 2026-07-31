package kr.inuappcenterportal.inuportal.domain.course.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "school.api")
public record SchoolApiProperties(
        String baseUrl,
        String authKey,
        String courseInfoPath,
        String courseMeetingInfoPath
) {
}
