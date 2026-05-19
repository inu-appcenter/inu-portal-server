package kr.inuappcenterportal.inuportal.domain.chat.config;

import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import kr.inuappcenterportal.inuportal.domain.chat.service.ChatRoomService;
import java.util.Map;
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
    
    @Autowired
    @Lazy
    private ChatRoomService chatRoomService;
    
    private final kr.inuappcenterportal.inuportal.domain.chat.repository.ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
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
            
            // 2. SUBSCRIBE 감지하여 방 검증 및 Redis 자동 등록
            else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                String destination = accessor.getDestination();
                if (destination != null && destination.startsWith("/sub/room/")) {
                    Long roomId = extractRoomId(destination);
                    Long memberId = getMemberId(accessor);

                    // 해당 채팅방 참여자인지 검증 (JOINED 상태여야 함)
                    boolean isMember = chatRoomMemberRepository.existsByChatRoomIdAndMemberIdAndStatus(roomId, memberId, kr.inuappcenterportal.inuportal.domain.chat.enums.ChatMemberStatus.JOINED);
                    if (!isMember) {
                        log.warn("[STOMP] 권한 없는 구독 시도 차단: Member {}, Room {}", memberId, roomId);
                        throw new AccessDeniedException("해당 채팅방에 참여할 권한이 없습니다.");
                    }

                    // 세션 속성에 roomId, memberId 저장 (Disconnect/Unsubscribe 시 자동 해제 목적)
                    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                    if (sessionAttributes != null) {
                        sessionAttributes.put("roomId", roomId);
                        sessionAttributes.put("memberId", memberId);
                    }

                    // Redis 및 채팅방 진입 자동 활성화 (안 읽음 처리 및 브로드캐스트 포함)
                    chatRoomService.enterChatRoom(roomId, memberId);
                    log.info("[STOMP] 구독 감지로 접속 활성화: Member {} -> Room {}", memberId, roomId);
                }
            }

            // 3. UNSUBSCRIBE 감지하여 Redis 자동 해제
            else if (StompCommand.UNSUBSCRIBE.equals(accessor.getCommand())) {
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                if (sessionAttributes != null && sessionAttributes.containsKey("roomId") && sessionAttributes.containsKey("memberId")) {
                    Long roomId = (Long) sessionAttributes.get("roomId");
                    Long memberId = (Long) sessionAttributes.get("memberId");

                    chatRoomService.exitChatRoom(roomId, memberId);
                    log.info("[STOMP] 구독 해제 감지로 접속 비활성화: Member {} -> Room {}", memberId, roomId);
                }
            }
        }

        return message;
    }

    private Long extractRoomId(String destination) {
        try {
            return Long.parseLong(destination.replace("/sub/room/", ""));
        } catch (NumberFormatException e) {
            throw new AccessDeniedException("올바르지 않은 구독 경로입니다.");
        }
    }

    private Long getMemberId(StompHeaderAccessor accessor) {
        if (accessor.getUser() != null) {
            return Long.parseLong(accessor.getUser().getName());
        }
        throw new AccessDeniedException("인증 정보가 유효하지 않습니다.");
    }
}
