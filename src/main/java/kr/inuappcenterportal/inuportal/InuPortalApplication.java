package kr.inuappcenterportal.inuportal;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
@OpenAPIDefinition(
        servers = {
                @Server(url = "http://portal.inuappcenter.kr/", description = "Server URL"),
                @Server(url = "http://localhost:8080",description = "Local URL")
        }
)
@EnableJpaAuditing
@SpringBootApplication
public class InuPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(InuPortalApplication.class, args);
    }

}
