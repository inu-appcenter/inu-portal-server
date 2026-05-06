package kr.inuappcenterportal.inuportal.domain.chat.service;

import kr.inuappcenterportal.inuportal.domain.chat.dto.ChatMessageDto;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatNotificationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final FcmService fcmService;
    private final MemberRepository memberRepository;
    private final ChatRoomService chatRoomService;
    private final WebSocketSessionManager sessionManager;

    private static final int BATCH_SIZE = 5;
    private static final long BATCH_TIMEOUT_MINUTES = 10; // 메시지 묶음 최대 대기 시간 (분)
    private static final long BATCH_RESET_SECONDS = 120; // 묶음 모드 만료 시간 재설정 (초)
    private static final long IMMEDIATE_TO_BATCH_THRESHOLD_SECONDS = 30; // 즉시->묶음 모드 전환 임계값 (초)

    private static final String MODE_IMMEDIATE = "IMMEDIATE";
    private static final String MODE_BATCHING = "BATCHING";
    private static final String MESSAGE_DELIMITER = "::"; // 닉네임, 메시지 내용 구분자

    public void processChatMessage(ChatMessageDto message) {
        Member sender = memberRepository.findByNickname(message.getSender()).orElse(null);
        if (sender == null) {
            log.warn("메시지 발신자 정보를 찾을 수 없습니다: {}", message.getSender());
            return;
        }

        List<Member> roomMembers = chatRoomService.getRoomMembers(message.getRoomId());

        for (Member recipient : roomMembers) {
            if (Objects.equals(sender.getId(), recipient.getId()) || !recipient.isChatNotification()) {
                continue;
            }

            // 수신자 채팅방 접속 여부 확인
            if (sessionManager.isUserConnected(recipient.getId(), message.getRoomId())) {
                log.info("사용자(id:{})가 채팅방(id:{})에 접속 중이므로 푸시 알림을 보내지 않습니다.", recipient.getId(), message.getRoomId());
                continue;
            }

            String modeKey = "chat:mode:" + message.getRoomId() + ":" + recipient.getId();
            String lastMsgTimeKey = "chat:last_msg_time:" + message.getRoomId() + ":" + recipient.getId();
            String listKey = "chat:notification:" + message.getRoomId() + ":" + recipient.getId();

            String currentMode = redisTemplate.opsForValue().get(modeKey);
            String lastMsgTimestampStr = redisTemplate.opsForValue().get(lastMsgTimeKey);
            Long lastMsgTimestamp = lastMsgTimestampStr != null ? Long.parseLong(lastMsgTimestampStr) : null;
            long currentTime = Instant.now().getEpochSecond();

            redisTemplate.opsForValue().set(lastMsgTimeKey, String.valueOf(currentTime));
            redisTemplate.expire(lastMsgTimeKey, BATCH_TIMEOUT_MINUTES + 1, TimeUnit.MINUTES); // 모드 키보다 긴 만료시간 설정

            // Redis 저장 형식: "닉네임::메시지"
            String messageToStore = message.getSender() + MESSAGE_DELIMITER + message.getMessage();

            if (MODE_BATCHING.equals(currentMode)) {
                // 묶음 발송 모드
                redisTemplate.opsForList().rightPush(listKey, messageToStore);
                redisTemplate.expire(listKey, BATCH_RESET_SECONDS, TimeUnit.SECONDS);

                Long size = redisTemplate.opsForList().size(listKey);
                if (size != null && size >= BATCH_SIZE) {
                    sendNotification(listKey, message.getRoomId(), recipient);
                    redisTemplate.opsForValue().set(modeKey, MODE_IMMEDIATE); // 즉시 발송 모드로 전환
                    redisTemplate.expire(modeKey, BATCH_TIMEOUT_MINUTES + 1, TimeUnit.MINUTES);
                }
            } else {
                // 즉시 발송 모드
                long interval = (lastMsgTimestamp != null) ? (currentTime - lastMsgTimestamp) : Long.MAX_VALUE;

                if (interval <= IMMEDIATE_TO_BATCH_THRESHOLD_SECONDS) {
                    // 메시지 간격 짧을 경우, 묶음 모드로 전환
                    log.info("메시지 간격이 짧아 묶음 모드로 전환: memberId={}, roomId={}", recipient.getId(), message.getRoomId());
                    redisTemplate.opsForValue().set(modeKey, MODE_BATCHING);
                    redisTemplate.expire(modeKey, BATCH_TIMEOUT_MINUTES + 1, TimeUnit.MINUTES);

                    redisTemplate.opsForList().rightPush(listKey, messageToStore);
                    redisTemplate.expire(listKey, BATCH_RESET_SECONDS, TimeUnit.SECONDS);
                } else {
                    // 메시지 간격 길 경우, 즉시 알림 발송
                    log.info("메시지 간격이 길어 즉시 알림 발송: memberId={}, roomId={}", recipient.getId(), message.getRoomId());
                    fcmService.sendUntrackedNotification(
                            List.of(recipient.getId()),
                            message.getRoomId(), // 제목: 채팅방 이름
                            message.getSender() + ": " + message.getMessage() // 내용: sendername: 메시지내용
                    );
                    redisTemplate.opsForValue().set(modeKey, MODE_IMMEDIATE); // 즉시 발송 모드 유지
                    redisTemplate.expire(modeKey, BATCH_TIMEOUT_MINUTES + 1, TimeUnit.MINUTES);
                }
            }
        }
    }

    public void sendNotification(String redisKey, String roomId, Member recipient) {
        // 수신자 접속 여부 이중 확인
        if (sessionManager.isUserConnected(recipient.getId(), roomId)) {
            redisTemplate.delete(redisKey);
            log.info("사용자(id:{})가 채팅방(id:{})에 접속 중이므로 푸시 알림을 보내지 않습니다. (만료 이벤트)", recipient.getId(), roomId);
            return;
        }

        List<String> rawMessages = redisTemplate.opsForList().range(redisKey, 0, -1);
        if (rawMessages == null || rawMessages.isEmpty()) {
            redisTemplate.delete(redisKey);
            return;
        }

        StringBuilder bodyBuilder = new StringBuilder();
        int messagesCount = rawMessages.size();
        int displayCount = Math.min(messagesCount, BATCH_SIZE); // 알림에 표시할 최대 메시지 수

        for (int i = 0; i < displayCount; i++) {
            String rawMessage = rawMessages.get(i);
            String[] parts = rawMessage.split(MESSAGE_DELIMITER, 2);
            String senderNickname = parts.length > 0 ? parts[0] : "알 수 없음";
            String messageContent = parts.length > 1 ? parts[1] : rawMessage;

            bodyBuilder.append(senderNickname).append(": ").append(messageContent);
            if (i < displayCount - 1) {
                bodyBuilder.append("\n");
            }
        }

        if (messagesCount > BATCH_SIZE) {
            bodyBuilder.append("\n외 ").append(messagesCount - BATCH_SIZE).append("개 메시지");
        }
        String body = bodyBuilder.toString();

        fcmService.sendUntrackedNotification(List.of(recipient.getId()), roomId, body); // roomId를 직접 전달

        // 발송 후 데이터 및 모드 초기화
        redisTemplate.delete(redisKey);
        String modeKey = "chat:mode:" + roomId + ":" + recipient.getId();
        String lastMsgTimeKey = "chat:last_msg_time:" + roomId + ":" + recipient.getId();
        redisTemplate.delete(modeKey);
        redisTemplate.delete(lastMsgTimeKey);

        log.info("채팅 알림 발송 완료: 수신자 ID={}, 채팅방 ID={}, 메시지 {}개", recipient.getId(), roomId, messagesCount);
    }
}
