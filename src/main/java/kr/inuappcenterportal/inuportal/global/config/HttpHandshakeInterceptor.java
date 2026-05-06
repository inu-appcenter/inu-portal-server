package kr.inuappcenterportal.inuportal.global.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpHandshakeInterceptor implements HandshakeInterceptor {

    private final TokenProvider tokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();

            // 토큰 추출
            String token = tokenProvider.resolveToken(httpServletRequest);

            if (token != null && tokenProvider.validateToken(token)) {
                String memberIdStr = tokenProvider.getUsername(token);
                try {
                    Long memberId = Long.parseLong(memberIdStr);
                    attributes.put("memberId", memberId);
                    // 인증 성공 및 연결 로그
                    log.info("사용자 {} 인증 완료 및 웹소켓 연결 성공", memberId);
                    return true;
                } catch (NumberFormatException e) {
                    // 식별자 형식 오류 로그
                    log.error("토큰 내 사용자 식별자 형식 오류: {}", memberIdStr);
                }
            } else {
                // 인증 실패 로그
                log.warn("웹소켓 핸드셰이크 실패: JWT 토큰 누락 또는 유효하지 않음");
            }
        }
        return false; // 인증 실패 시 연결 거부
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // 핸드셰이크 완료 후 로직
    }
}