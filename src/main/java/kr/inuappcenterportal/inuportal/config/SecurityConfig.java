package kr.inuappcenterportal.inuportal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.exception.CustomAccessDeniedHandler;
import kr.inuappcenterportal.inuportal.exception.CustomAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

        httpSecurity.
                csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        httpSecurity
                .authorizeHttpRequests(auth->auth.requestMatchers("/swagger", "/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/members/**","/api/members").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/reports/**").hasAnyRole("USER","ADMIN")
                        .requestMatchers("/api/members/all","/api/reports").hasRole("ADMIN")
                        .requestMatchers("/api/members/**","/api/members").hasAnyRole("USER","ADMIN")
                        .requestMatchers(HttpMethod.GET,"/api/posts/**","/api/posts","/api/cafeterias","/api/weathers").permitAll()
                        .requestMatchers("/api/posts/**","/api/posts","/api/fires/**").hasAnyRole("USER","ADMIN")
                        .requestMatchers("/api/replies/**").hasAnyRole("USER","ADMIN")
                        .requestMatchers("/api/search","/api/notices","/api/notices/**","api/schedules","api/schedules/**").permitAll()
                        .requestMatchers("/api/folders/**","/api/folders","/api/search/**").hasAnyRole("USER","ADMIN")
                        .requestMatchers(HttpMethod.GET,"/api/categories","/api/images/**").permitAll()
                        .requestMatchers("/api/images","/api/images/**","/api/categories").hasRole("ADMIN"));
        httpSecurity
                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider,objectMapper), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception->exception.accessDeniedHandler(new CustomAccessDeniedHandler(objectMapper))
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint(objectMapper)));

        return httpSecurity.build();
    }


}
