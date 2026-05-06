package kr.inuappcenterportal.inuportal.domain.chat.service;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisKeyExpirationListener implements MessageListener {

    private final ChatNotificationService chatNotificationService;
    private final MemberRepository memberRepository;

    /**
     * Redis 키 만료 이벤트 수신 및 처리
     * @param message 만료된 키 정보
     * @param pattern 구독 패턴
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();

        // 채팅 알림 관련 키만 처리
        if (expiredKey.startsWith("chat:notification:")) {
            String[] parts = expiredKey.split(":");
            if (parts.length == 4) {
                String roomId = parts[2];
                Long memberId = Long.parseLong(parts[3]);

                memberRepository.findById(memberId).ifPresent(member -> {
                    chatNotificationService.sendNotification(expiredKey, roomId, member);
                });
            } else {
                log.warn("만료된 Redis 키 형식이 예상과 다릅니다: {}", expiredKey);
            }
        }
    }
}
