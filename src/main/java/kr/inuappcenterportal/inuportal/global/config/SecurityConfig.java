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
    public SecurityConfig(TokenProvider tokenProvider, ObjectMapper objectMapper) {
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        httpSecurity
                .authorizeHttpRequests(auth -> auth
                        // SSE 및 비동기/에러 디스패처 허용
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ASYNC, jakarta.servlet.DispatcherType.ERROR).permitAll()
                        // 최우선 허용 설정
                        .requestMatchers(HttpMethod.GET, "/api/chat-rooms/*/messages/public").permitAll()
                        // 친구추가 링크는 비로그인 상태에서도 "누구의 링크인지" 미리보기가 가능해야 한다.
                        .requestMatchers(HttpMethod.GET, "/api/friends/invite/*").permitAll()
                        .requestMatchers("/swagger", "/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**", "/v3/api-docs/**", "/images/**", "/actuator/**", "/ws-chat/**", "/api/search", "/api/notices", "/api/notices/**", "/api/schedules", "/api/schedules/**", "/error").permitAll()

                        // 공통 GET 허용 설정
                        .requestMatchers(HttpMethod.GET, "/api/posts/**", "/api/posts", "/api/cafeterias", "/api/weathers", "/api/buses/**", "/api/buses", "/api/councilNotices", "/api/councilNotices/**", "/api/petitions", "/api/petitions/**", "/api/reservations/quantity/**", "/api/directory", "/api/directory/**", "/api/categories/**", "/api/images/**", "/api/books/**", "/api/items/**", "/api/lost/**", "/api/clubs", "/api/clubs/**", "/api/feature-flags", "/api/semesters").permitAll()

                        // 인증 없이 접근 가능한 POST 설정
                        .requestMatchers(HttpMethod.POST, "/api/members/**", "/api/members", "/api/tokens", "/api/logs/**").permitAll()

                        // ADMIN 권한 전용 설정
                        .requestMatchers(HttpMethod.POST, "/api/directory/sync", "/api/directory/sources/sync", "/api/directory/college-office-contacts/sync", "/api/semesters/sync", "/api/courses/sync", "/api/course-offerings/sync", "/api/course-offerings/legacy").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/logs/**").hasRole("ADMIN")
                        .requestMatchers("/api/members/all", "/api/reports", "/api/images", "/api/images/**", "/api/categories", "/api/councilNotices", "/api/councilNotices/**", "/api/books/**", "/api/items/**", "/api/lost/**", "/api/clubs/**", "/api/tokens/admin/**", "/api/admin/feature-flags/**", "/api/admin/buses/**").hasRole("ADMIN")


                        // USER 또는 ADMIN 권한 설정
                        .requestMatchers(HttpMethod.POST, "/api/reports/**", "/api/portal/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/chat-rooms/**", "/api/members/**", "/api/members", "/api/friends", "/api/friends/**", "/api/tokens", "/api/posts/**", "/api/posts", "/api/fires/**", "/api/petitions", "/api/petitions/**", "/api/replies/**", "/api/reservations/**", "/api/folders/**", "/api/folders", "/api/search/**", "/api/keywords/**").hasAnyRole("USER", "ADMIN")

                        // 추가 인증 필요 설정
                        .requestMatchers(HttpMethod.POST, "/api/chat/messages").authenticated()

                        // 나머지 모든 요청
                        .anyRequest().authenticated()
                );

        httpSecurity
                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider, objectMapper), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception.accessDeniedHandler(new CustomAccessDeniedHandler(objectMapper))
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint(objectMapper)));

        return httpSecurity.build();
    }
}