package kr.inuappcenterportal.inuportal.domain.chat.repository;

import kr.inuappcenterportal.inuportal.domain.chat.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByName(String name);
}
