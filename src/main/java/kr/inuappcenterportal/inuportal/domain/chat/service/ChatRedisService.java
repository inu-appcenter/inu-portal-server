package kr.inuappcenterportal.inuportal.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String ROOM_USERS_KEY_PREFIX = "room:%d:users";
    private static final String ROOM_ANON_MAP_KEY_PREFIX = "room:%d:anon_map"; // memberId -> anonNum
    private static final String ROOM_ANON_NUMBERS_KEY_PREFIX = "room:%d:anon_numbers"; // 사용된 익명 번호 Set
    private static final String ROOM_ADMIN_MAP_KEY_PREFIX = "room:%d:admin_map"; // memberId -> adminNum
    private static final String ROOM_MESSAGES_KEY_PREFIX = "room:%d:messages";
    private static final int MAX_ANON_NUMBER = 9999;
    private static final int MAX_RETRY_COUNT = 10; // 무한 루프 방지

    /**
     * 익명 닉네임 조회 및 할당 (랜덤 번호 방식)
     */
    public String getOrAssignAnonymousNickname(Long roomId, Long memberId) {
        String mapKey = String.format(ROOM_ANON_MAP_KEY_PREFIX, roomId);
        String memberIdStr = String.valueOf(memberId);

        // 기존에 할당된 번호가 있는지 확인
        Object assignedNum = redisTemplate.opsForHash().get(mapKey, memberIdStr);
        if (assignedNum != null) {
            return "익명" + assignedNum;
        }

        // 새로운 랜덤 번호 할당
        String numbersKey = String.format(ROOM_ANON_NUMBERS_KEY_PREFIX, roomId);
        int retryCount = 0;

        while (retryCount < MAX_RETRY_COUNT) {
            int randomNum = ThreadLocalRandom.current().nextInt(1, MAX_ANON_NUMBER + 1);
            String randomNumStr = String.valueOf(randomNum);

            if (redisTemplate.opsForSet().add(numbersKey, randomNumStr) == 1) {
                // 사용자에게 번호 할당 정보 저장
                redisTemplate.opsForHash().put(mapKey, memberIdStr, randomNumStr);
                return "익명" + randomNumStr;
            }
            retryCount++;
        }

        // 재시도 후에도 고유 번호를 찾지 못한 경우 예외 처리
        long fallbackNum = System.currentTimeMillis() % 10000;
        redisTemplate.opsForHash().put(mapKey, memberIdStr, String.valueOf(fallbackNum));
        log.warn("채팅방 {}에서 랜덤 익명 번호 할당에 실패하여 폴백 로직을 사용합니다.", roomId);
        return "익명" + fallbackNum;
    }

    /**
     * 익명 닉네임 복사/연동
     */
    public void copyAnonymousNickname(Long sourceRoomId, Long targetRoomId, Long memberId) {
        String sourceMapKey = String.format(ROOM_ANON_MAP_KEY_PREFIX, sourceRoomId);
        String targetMapKey = String.format(ROOM_ANON_MAP_KEY_PREFIX, targetRoomId);
        String memberIdStr = String.valueOf(memberId);

        Object assignedNum = redisTemplate.opsForHash().get(sourceMapKey, memberIdStr);
        if (assignedNum != null) {
            String randomNumStr = String.valueOf(assignedNum);
            String targetNumbersKey = String.format(ROOM_ANON_NUMBERS_KEY_PREFIX, targetRoomId);
            redisTemplate.opsForSet().add(targetNumbersKey, randomNumStr);
            redisTemplate.opsForHash().put(targetMapKey, memberIdStr, randomNumStr);
        }
    }

    /**
     * 운영자 닉네임 조회 및 할당 (순차 번호 방식)
     */
    public String getOrAssignAdminNickname(Long roomId, Long memberId) {
        String mapKey = String.format(ROOM_ADMIN_MAP_KEY_PREFIX, roomId);
        String memberIdStr = String.valueOf(memberId);

        // 기존에 할당된 번호가 있는지 확인
        Object assignedNum = redisTemplate.opsForHash().get(mapKey, memberIdStr);
        if (assignedNum != null) {
            return "운영자" + assignedNum;
        }

        // 새로운 순차 번호 할당 (현재 매핑된 수 + 1)
        Long nextNum = redisTemplate.opsForHash().size(mapKey) + 1;
        String nextNumStr = String.valueOf(nextNum);

        redisTemplate.opsForHash().put(mapKey, memberIdStr, nextNumStr);
        return "운영자" + nextNumStr;
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
     * 현재 채팅방 접속자 ID 목록 조회
     */
    public Set<String> getRoomUserIds(Long roomId) {
        String key = String.format(ROOM_USERS_KEY_PREFIX, roomId);
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 최근 메시지 캐싱 (최대 50개 유지)
     */
    public void saveMessageToCache(Long roomId, String messageJson) {
        String key = String.format(ROOM_MESSAGES_KEY_PREFIX, roomId);
        redisTemplate.opsForList().rightPush(key, messageJson);
        redisTemplate.opsForList().trim(key, -50, -1); // 최근 50개만 남김
    }

    /**
     * 최근 메시지 목록 조회
     */
    public List<String> getRecentMessages(Long roomId) {
        String key = String.format(ROOM_MESSAGES_KEY_PREFIX, roomId);
        return redisTemplate.opsForList().range(key, 0, -1);
    }
}
