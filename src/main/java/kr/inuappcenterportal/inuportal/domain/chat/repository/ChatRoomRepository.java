package kr.inuappcenterportal.inuportal.domain.chat.repository;

import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
}
