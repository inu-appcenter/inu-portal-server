package kr.inuappcenterportal.inuportal.global.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI(){
        return new OpenAPI()
                .components(new Components())
                .servers(List.of(currentEnvironmentServer()))
                .info(apiInfo());
    }

    private Server currentEnvironmentServer() {
        return new Server()
                .url("/")
                .description("Current environment");
    }

    private Info apiInfo() {
        return new Info()
                .title("INTIP API명세서")
                .description("이미지 호출 : /images/도메인/id-이미지번호  ex) /images/post-1-1 \n 썸네일 호출 : /images/도메인/thumbnail/id  ex /images/post/thumbnail/1")
                .version("1.0.0");
    }
}
