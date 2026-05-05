package kr.inuappcenterportal.inuportal.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final ObjectMapper objectMapper;
    private final RequestMatcher permitAllRequestMatcher;

    public JwtAuthenticationFilter(TokenProvider tokenProvider, ObjectMapper objectMapper, String[] permitAllPatterns){
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
        List<RequestMatcher> matchers = Arrays.stream(permitAllPatterns)
                .map(AntPathRequestMatcher::new)
                .collect(Collectors.toList());
        this.permitAllRequestMatcher = new OrRequestMatcher(matchers);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // If the request URI matches a permitAll path, skip this filter entirely
        if (permitAllRequestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

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
