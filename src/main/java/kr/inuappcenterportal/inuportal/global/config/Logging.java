package kr.inuappcenterportal.inuportal.global.config;

import jakarta.servlet.http.HttpServletRequest;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;

@Slf4j
@Aspect
@Component
public class Logging {
    private static final Set<String> except_uri = Set.of(
            "/api/weathers",
            "/api/notices/top",
            "/api/posts/main",
            "/api/members/refresh"
    );
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restControllerMethods() {}

    @Around("restControllerMethods()")
    public Object logApiRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String uri = request.getRequestURI();
        String httpMethod = request.getMethod();

        if (except_uri.contains(uri)) {

            return joinPoint.proceed();
        }
        String memberId = getMemberId(request);

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - startTime;
        log.info("user={} {} {} {}ms", memberId, httpMethod, uri, duration);

        return result;
    }
    private String getMemberId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof String|| authentication.getPrincipal() == null) {
            return request.getHeader("X-Forwarded-For");
        }
        Member member = (Member)authentication.getPrincipal();
        return String.valueOf(member.getId());
    }
}
