package kr.inuappcenterportal.inuportal.domain.chat.repository;

import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatMessage;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop50ByChatRoomOrderByCreateDateDesc(ChatRoom chatRoom);
    List<ChatMessage> findTop2ByChatRoomOrderByCreateDateDesc(ChatRoom chatRoom); // 최신 메시지 2개 조회
}
