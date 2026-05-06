package kr.inuappcenterportal.inuportal.domain.chat.config;

import kr.inuappcenterportal.inuportal.global.config.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class StompHandler implements ChannelInterceptor {

    private final TokenProvider tokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Auth");

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            if (token != null && tokenProvider.validateToken(token)) {
                Authentication authentication = tokenProvider.getAuthentication(token);
                accessor.setUser(authentication);
                log.info("웹소켓 연결 성공: 사용자={}", authentication.getName());
            } else {
                log.error("웹소켓 인증 실패: 유효하지 않은 토큰이거나 인증 헤더가 누락되었습니다. 토큰={}", token);
                throw new AccessDeniedException("유효하지 않은 토큰입니다.");
            }
        }

        // TODO: SUBSCRIBE 명령어일 경우, 해당 채팅방의 실제 참여자인지 DB/Redis를 통해 확인하는 로직 추가 가능
        
        return message;
    }
}
