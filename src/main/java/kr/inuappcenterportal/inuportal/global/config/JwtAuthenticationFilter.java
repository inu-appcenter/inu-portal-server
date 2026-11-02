package kr.inuappcenterportal.inuportal.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // 토큰 검증 실패 사유(MyErrorCode). CustomAuthenticationEntryPoint가 꺼내 기존과 같은 응답을 쓴다.
    public static final String AUTH_ERROR_ATTRIBUTE = "jwtAuthError";

    private final TokenProvider tokenProvider;

    public JwtAuthenticationFilter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    /**
     * 들어온 요청의 JWT 토큰을 검증
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // 헤더(Auth)에서 토큰을 꺼냄
            String token = tokenProvider.resolveToken(request);

            // 토큰 파싱 + 서명/만료 검증 → 통과하면 회원 조회 후 인증 정보 등록
            if (token != null && tokenProvider.validateToken(token)) {
                Authentication authentication = tokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (MyException e) {
            // 익명으로 체인을 계속 진행하고, 인증이 필요한 엔드포인트에서만 EntryPoint가 사유를 담아 응답
            SecurityContextHolder.clearContext();
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, e.getErrorCode());
        }
        filterChain.doFilter(request, response);
    }
}
