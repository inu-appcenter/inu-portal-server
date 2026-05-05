package kr.inuappcenterportal.inuportal.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String ROOM_USERS_KEY_PREFIX = "room:%d:users";
    private static final String ROOM_ANON_MAP_KEY_PREFIX = "room:%d:anon_map";
    private static final String ROOM_ANON_SEQ_KEY_PREFIX = "room:%d:anon_seq";
    private static final String ROOM_MESSAGES_KEY_PREFIX = "room:%d:messages";

    /**
     * 익명 닉네임 조회 및 할당
     */
    public String getOrAssignAnonymousNickname(Long roomId, Long memberId) {
        String mapKey = String.format(ROOM_ANON_MAP_KEY_PREFIX, roomId);
        String seqKey = String.format(ROOM_ANON_SEQ_KEY_PREFIX, roomId);
        
        Object assignedNum = redisTemplate.opsForHash().get(mapKey, String.valueOf(memberId));
        if (assignedNum != null) {
            return "익명" + assignedNum;
        }

        Long newSeq = redisTemplate.opsForValue().increment(seqKey);
        redisTemplate.opsForHash().put(mapKey, String.valueOf(memberId), String.valueOf(newSeq));
        return "익명" + newSeq;
    }

    /**
     * 채팅방 접속자 추가 (Set)
     */
    public void addUserToRoom(Long roomId, Long memberId) {
        String key = String.format(ROOM_USERS_KEY_PREFIX, roomId);
        redisTemplate.opsForSet().add(key, String.valueOf(memberId));
    }

    /**
     * 채팅방 접속자 제거 (Set)
     */
    public void removeUserFromRoom(Long roomId, Long memberId) {
        String key = String.format(ROOM_USERS_KEY_PREFIX, roomId);
        redisTemplate.opsForSet().remove(key, String.valueOf(memberId));
    }

    /**
     * 현재 채팅방 접속자 수 조회
     */
    public Long getRoomUserCount(Long roomId) {
        String key = String.format(ROOM_USERS_KEY_PREFIX, roomId);
        return redisTemplate.opsForSet().size(key);
    }

    /**
     * 최근 메시지 캐싱 (최대 50개 유지)
     */
    public void saveMessageToCache(Long roomId, String messageJson) {
        String key = String.format(ROOM_MESSAGES_KEY_PREFIX, roomId);
        redisTemplate.opsForList().rightPush(key, messageJson);
        redisTemplate.opsForList().trim(key, -50, -1); // 최근 50개만 남기기
    }

    /**
     * 최근 메시지 목록 조회
     */
    public List<String> getRecentMessages(Long roomId) {
        String key = String.format(ROOM_MESSAGES_KEY_PREFIX, roomId);
        return redisTemplate.opsForList().range(key, 0, -1);
    }
}
