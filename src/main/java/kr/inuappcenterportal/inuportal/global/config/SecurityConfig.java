package kr.inuappcenterportal.inuportal.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.global.exception.CustomAccessDeniedHandler;
import kr.inuappcenterportal.inuportal.global.exception.CustomAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final TokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    @Autowired
    public SecurityConfig(TokenProvider tokenProvider, ObjectMapper objectMapper){
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception{

        httpSecurity
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        httpSecurity
                .authorizeHttpRequests(auth -> auth
                        // 전체 허용 설정
                        .requestMatchers("/swagger", "/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**", "/v3/api-docs/**", "/images/**", "/actuator/**", "/ws-chat/**", "/api/search", "/api/notices", "/api/notices/**", "/api/schedules", "/api/schedules/**", "/{roomId}/messages/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/posts/**", "/api/posts", "/api/cafeterias", "/api/weathers", "/api/councilNotices", "/api/councilNotices/**", "/api/petitions", "/api/petitions/**", "/api/reservations/quantity/**", "/api/directory", "/api/directory/**", "/api/categories/**", "/api/images/**", "/api/books/**", "/api/items/**", "/api/lost/**", "/api/clubs", "/api/clubs/**", "/api/feature-flags").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/members/**", "/api/members", "/api/tokens", "/api/logs/**").permitAll()

                        // ADMIN 권한 설정
                        .requestMatchers(HttpMethod.POST, "/api/chat-rooms", "/api/directory/sync", "/api/directory/sources/sync", "/api/directory/college-office-contacts/sync").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/logs/**").hasRole("ADMIN")
                        .requestMatchers("/api/members/all", "/api/reports", "/api/images", "/api/images/**", "/api/categories", "/api/councilNotices", "/api/councilNotices/**", "/api/books/**", "/api/items/**", "/api/lost/**", "/api/clubs/**", "/api/tokens/admin/**", "/api/admin/feature-flags/**").hasRole("ADMIN")

                        // USER 또는 ADMIN 권한 설정
                        .requestMatchers(HttpMethod.POST, "/api/reports/**", "/api/portal/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/chat-rooms/**", "/api/members/**", "/api/members", "/api/tokens", "/api/posts/**", "/api/posts", "/api/fires/**", "/api/petitions", "/api/petitions/**", "/api/replies/**", "/api/reservations/**", "/api/folders/**", "/api/folders", "/api/search/**", "/api/keywords/**").hasAnyRole("USER", "ADMIN")

                        // 일반 인증 필요 설정
                        .requestMatchers(HttpMethod.POST, "/api/chat/messages").authenticated()

                        // 나머지 모든 요청 설정
                        .anyRequest().authenticated()
                );

        httpSecurity
                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider, objectMapper), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception.accessDeniedHandler(new CustomAccessDeniedHandler(objectMapper))
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint(objectMapper)));

        return httpSecurity.build();
    }
}