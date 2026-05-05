package kr.inuappcenterportal.inuportal.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {
    private static final List<Server> SERVERS = List.of(
            createServer("http://localhost:8080", "Local"),
            createServer("https://portal.inuappcenter.kr", "Production"),
            createServer("https://portal-dev.inuappcenter.kr", "Development")
    );

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("AuthToken", new SecurityScheme()
                                .name("Auth")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("JWT Access Token"))
                        .addSecuritySchemes("RefreshToken", new SecurityScheme()
                                .name("refresh")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("JWT Refresh Token")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("AuthToken")
                        .addList("RefreshToken"))
                .servers(SERVERS)
                .info(apiInfo());
    }

    private static Server createServer(String url, String description) {
        return new Server()
                .url(url)
                .description(description);
    }

    private Info apiInfo() {
        String description = """            
            1. 이미지 관련
            - 이미지: /images/{도메인}/{id}-{이미지번호} (예: /images/post/1-1)
            - 썸네일: /images/{도메인}/thumbnail/{id} (예: /images/post/thumbnail/1)
            
            2. 채팅(WebSocket)
            - 연결: /ws
            - 발행: /pub/message
            - 구독: /sub/room/{roomId}
            
            메시지 송수신 시 형식
            [송신 예시]
            {
              "roomId": 1,
              "content": "안녕하세요!",
              "isAnonymous": true
            }
            [수신 예시]
            {
              "roomId": 1,
              "senderNickname": "익명1",
              "senderHash": "a1b2c3d4...",
              "content": "안녕하세요!",
              "createDate": "2024-07-26T10:00:00"
            }
            """;

        return new Info()
                .title("INTIP API 명세서")
                .description(description)
                .version("1.0.0");
    }
}
