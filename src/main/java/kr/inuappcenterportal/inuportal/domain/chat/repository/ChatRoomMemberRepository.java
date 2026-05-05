package kr.inuappcenterportal.inuportal.domain.chat.repository;

import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoom;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;

import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    Optional<ChatRoomMember> findByChatRoomAndMember(ChatRoom chatRoom, Member member);
    boolean existsByChatRoomAndMember(ChatRoom chatRoom, Member member);
    int countByChatRoom(ChatRoom chatRoom);
}
