package kr.inuappcenterportal.inuportal.exception;

import com.google.gson.Gson;
import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        Gson gson = new Gson();
        log.info("사람 예외 발생");
        String result = gson.toJson(ResponseDto.of(401,"인증이 실패하였습니다."));
        response.setStatus(401);
        response.getWriter().write(result);
    }
}
