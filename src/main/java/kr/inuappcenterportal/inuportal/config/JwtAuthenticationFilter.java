package kr.inuappcenterportal.inuportal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(TokenProvider tokenProvider, ObjectMapper objectMapper){
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = tokenProvider.resolveToken(request);
            if (token!=null&&tokenProvider.validateToken(token)) {
                Authentication authentication = tokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        }
        catch (MyException e){
            String msg = e.getErrorCode().getMessage();
            if(MyErrorCode.WRONG_TYPE_TOKEN.getMessage().equals(msg)){
                setResponse(response,MyErrorCode.WRONG_TYPE_TOKEN);
            }
            else if(MyErrorCode.UNSUPPORTED_TOKEN.getMessage().equals(msg)){
                setResponse(response,MyErrorCode.UNSUPPORTED_TOKEN);
            }
            else if(MyErrorCode.EXPIRED_TOKEN.getMessage().equals(msg)){
                setResponse(response,MyErrorCode.EXPIRED_TOKEN);
            }
            else if(MyErrorCode.UNKNOWN_TOKEN_ERROR.getMessage().equals(msg)){
                setResponse(response,MyErrorCode.UNKNOWN_TOKEN_ERROR);
            }
            else if(MyErrorCode.USER_NOT_FOUND.getMessage().equals(msg)){
                setResponse(response,MyErrorCode.USER_NOT_FOUND);
            }
        }
    }

    private void setResponse(HttpServletResponse response, MyErrorCode myErrorCode) throws IOException {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(myErrorCode.getStatus().value());
            response.getWriter().print(objectMapper.writeValueAsString(ResponseDto.of(-1,myErrorCode.getMessage())));
    }

}
