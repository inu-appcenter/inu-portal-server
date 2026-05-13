package kr.inuappcenterportal.inuportal.domain.chat.listener;

import kr.inuappcenterportal.inuportal.domain.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final ChatRoomService chatRoomService;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        if (sessionAttributes != null && sessionAttributes.containsKey("roomId") && sessionAttributes.containsKey("memberId")) {
            Long roomId = (Long) sessionAttributes.get("roomId");
            Long memberId = (Long) sessionAttributes.get("memberId");

            log.info("[WebSocket] Session Disconnected: Member {} left Room {}", memberId, roomId);
            chatRoomService.exitChatRoom(roomId, memberId);
        }
    }
}
