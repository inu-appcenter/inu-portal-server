package kr.inuappcenterportal.inuportal.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketSessionManager {
    // 세션 ID -> 사용자 ID 매핑
    private final Map<String, Long> sessionToMemberId = new ConcurrentHashMap<>();
    // 사용자 ID -> 현재 접속 채팅방 ID 매핑
    private final Map<Long, String> memberIdToRoomId = new ConcurrentHashMap<>();

    /**
     * WebSocket 세션 등록
     * @param sessionId WebSocket 세션 ID
     * @param memberId 사용자 ID
     * @param roomId 채팅방 ID
     */
    public void registerSession(String sessionId, Long memberId, String roomId) {
        sessionToMemberId.put(sessionId, memberId);
        memberIdToRoomId.put(memberId, roomId);
        log.info("WebSocket Session Registered: memberId={}, roomId={}, sessionId={}", memberId, roomId, sessionId);
    }

    /**
     * WebSocket 세션 제거
     * @param sessionId WebSocket 세션 ID
     */
    public void removeSession(String sessionId) {
        Long memberId = sessionToMemberId.remove(sessionId);
        if (memberId != null) {
            memberIdToRoomId.remove(memberId);
            log.info("WebSocket Session Removed: memberId={}, sessionId={}", memberId, sessionId);
        }
    }

    /**
     * 사용자 채팅방 접속 여부 확인
     * @param memberId 확인할 사용자 ID
     * @param roomId 확인할 채팅방 ID
     * @return 접속 여부
     */
    public boolean isUserConnected(Long memberId, String roomId) {
        return roomId.equals(memberIdToRoomId.get(memberId));
    }
}
