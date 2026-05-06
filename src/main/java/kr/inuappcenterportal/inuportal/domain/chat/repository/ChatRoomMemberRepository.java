package kr.inuappcenterportal.inuportal.domain.chat.repository;

import kr.inuappcenterportal.inuportal.domain.chat.model.ChatRoomMember;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    @Query("SELECT crm.member FROM ChatRoomMember crm WHERE crm.chatRoom.id = :chatRoomId")
    List<Member> findAllMembersByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
