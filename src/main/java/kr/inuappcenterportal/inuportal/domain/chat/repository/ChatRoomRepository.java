package kr.inuappcenterportal.inuportal.domain.chat.repository;

import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT r FROM ChatRoom r " +
           "WHERE r.type = kr.inuappcenterportal.inuportal.domain.chat.enums.ChatRoomType.OPEN " +
           "AND r.status = kr.inuappcenterportal.inuportal.domain.chat.enums.ChatRoomStatus.ACTIVE " +
           "AND (:search IS NULL OR r.title LIKE %:search%) " +
           "ORDER BY r.createDate DESC")
    Page<ChatRoom> findOpenChatRooms(@Param("search") String search, Pageable pageable);
}
