package kr.inuappcenterportal.inuportal.config;

import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collections;
import java.util.List;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final TokenProvider tokenProvider;

    @Autowired
    public SecurityConfig(TokenProvider tokenProvider){
        this.tokenProvider = tokenProvider;
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception{

        httpSecurity.
                csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        httpSecurity
                .authorizeHttpRequests(auth->auth.requestMatchers("/swagger", "/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/members/**","/api/members").permitAll()
                        .requestMatchers("/api/members/all").hasRole("ADMIN")
                        .requestMatchers("/api/members/**","/api/members").hasAnyRole("USER","ADMIN")
                        .requestMatchers(HttpMethod.GET,"/api/posts/**","/api/posts","/api/fires/**","/api/cafeterias","/api/weathers").permitAll()
                        .requestMatchers("/api/posts/**","/api/posts","/api/fires/**","/api/fires").hasAnyRole("USER","ADMIN")
                        .requestMatchers("/api/replies/**").hasAnyRole("USER","ADMIN")
                        .requestMatchers("/api/search","/api/search/**","/api/notices","/api/notices/**","api/schedules","api/schedules/**").permitAll()
                        .requestMatchers("/api/folders/**","/api/folders").hasAnyRole("USER","ADMIN")
                        .requestMatchers(HttpMethod.GET,"/api/categories","/api/images/**").permitAll()
                        .requestMatchers("/api/images","/api/images/**","/api/categories","/api/fires/uri").hasRole("ADMIN"));
        httpSecurity
                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception->exception.accessDeniedHandler(new CustomAccessDeniedHandler())
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint()));

        return httpSecurity.build();
    }


}
