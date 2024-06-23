package kr.inuappcenterportal.inuportal;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@OpenAPIDefinition(
        servers = {
                @Server(url = "https://portal.inuappcenter.kr/", description = "Server URL"),
                @Server(url = "http://localhost:8080",description = "Local URL")
        },
        security = {
                @SecurityRequirement(name = "Auth"),
                @SecurityRequirement(name = "refresh")
        }
)
@SecuritySchemes({
        @SecurityScheme(name = "Auth",
                type = SecuritySchemeType.APIKEY,
                description = "JWT token",
                in = SecuritySchemeIn.HEADER,
                paramName = "Auth"),
        @SecurityScheme(name = "refresh",
                type = SecuritySchemeType.APIKEY,
                description = "JWT refresh token",
                in = SecuritySchemeIn.HEADER,
                paramName = "refresh")
})
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@EnableCaching
@SpringBootApplication
public class InuPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(InuPortalApplication.class, args);
    }

}
