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
                .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        httpSecurity
                .authorizeHttpRequests(auth->auth.requestMatchers("/swagger", "/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**", "/v3/api-docs/**","/images/**","/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/members/**","/api/members","/api/tokens").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/reports/**").hasAnyRole("USER","ADMIN")
                        .requestMatchers("/api/members/all","/api/reports").hasRole("ADMIN")
                        .requestMatchers("/api/members/**","/api/members","/api/tokens").hasAnyRole("USER","ADMIN")
                        .requestMatchers(HttpMethod.GET,"/api/posts/**","/api/posts","/api/cafeterias","/api/weathers","/api/councilNotices","/api/councilNotices/**","/api/petitions","/api/petitions/**","/api/reservations/quantity/**").permitAll()
                        .requestMatchers("/api/posts/**","/api/posts","/api/fires/**","/api/petitions","/api/petitions/**").hasAnyRole("USER","ADMIN")
                        .requestMatchers("/api/replies/**","/api/reservations/**").hasAnyRole("USER","ADMIN")
                        .requestMatchers("/api/search","/api/notices","/api/notices/**","api/schedules","api/schedules/**").permitAll()
                        .requestMatchers("/api/folders/**","/api/folders","/api/search/**").hasAnyRole("USER","ADMIN")
                        .requestMatchers(HttpMethod.GET,"/api/categories","/api/images/**", "/api/books/**", "/api/items/**", "/api/lost/**","/api/clubs","/api/clubs/**").permitAll()
                        .requestMatchers("/api/images","/api/images/**","/api/categories","/api/councilNotices","/api/councilNotices/**","/api/books/**", "/api/items/**", "/api/lost/**","/api/clubs/**").hasRole("ADMIN")
                        .requestMatchers("/api/keywords/**").hasAnyRole("USER","ADMIN")
                        .requestMatchers("/api/tokens/admin").hasRole("ADMIN")
                );
        httpSecurity
                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider,objectMapper), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception->exception.accessDeniedHandler(new CustomAccessDeniedHandler(objectMapper))
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint(objectMapper)));

        return httpSecurity.build();
    }


}
