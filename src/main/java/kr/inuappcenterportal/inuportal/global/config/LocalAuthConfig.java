package kr.inuappcenterportal.inuportal.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LocalAuthProperties.class)
public class LocalAuthConfig {
}
