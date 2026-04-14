package kr.inuappcenterportal.inuportal.global.config;

import kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.SecondDormitoryInstagramProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SecondDormitoryInstagramProperties.class)
public class SecondDormitoryInstagramConfig {
}
