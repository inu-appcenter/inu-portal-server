package kr.inuappcenterportal.inuportal.domain.chat.repository;

import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoomMember;
import kr.inuappcenterportal.inuportal.domain.chat.enums.ChatMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoom;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    Optional<ChatRoomMember> findByChatRoomAndMember(ChatRoom chatRoom, Member member);
    boolean existsByChatRoomAndMember(ChatRoom chatRoom, Member member);
    int countByChatRoom(ChatRoom chatRoom);
    List<ChatRoomMember> findAllByMemberAndStatus(Member member, ChatMemberStatus status);
    List<ChatRoomMember> findAllByChatRoomAndStatus(ChatRoom chatRoom, ChatMemberStatus status);
    long countByChatRoomAndLastReadMessageIdLessThan(ChatRoom chatRoom, Long messageId);
}
