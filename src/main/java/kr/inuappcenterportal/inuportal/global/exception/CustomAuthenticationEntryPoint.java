package kr.inuappcenterportal.inuportal.global.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.inuappcenterportal.inuportal.global.config.JwtAuthenticationFilter;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        // 토큰이 있었지만 검증에 실패한 경우: 클라이언트가 만료 메시지로 토큰 재발급을 판단하므로 기존 필터 응답 형식을 유지한다.
        if (request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE) instanceof MyErrorCode errorCode) {
            response.setStatus(errorCode.getStatus().value());
            response.getWriter().write(objectMapper.writeValueAsString(ResponseDto.of(-1, errorCode.getMessage())));
            return;
        }
        String result = objectMapper.writeValueAsString(ResponseDto.of(401,"인증이 실패하였습니다."));
        response.setStatus(401);
        response.getWriter().write(result);
    }
}
