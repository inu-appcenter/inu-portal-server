package kr.inuappcenterportal.inuportal.domain.chat.repository;

import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoomMember;
import kr.inuappcenterportal.inuportal.domain.chat.enums.ChatMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoom;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    Optional<ChatRoomMember> findByChatRoomAndMember(ChatRoom chatRoom, Member member);
    boolean existsByChatRoomAndMember(ChatRoom chatRoom, Member member);
    boolean existsByChatRoomIdAndMemberIdAndStatus(Long chatRoomId, Long memberId, ChatMemberStatus status);
    int countByChatRoom(ChatRoom chatRoom);
    List<ChatRoomMember> findAllByMemberAndStatus(Member member, ChatMemberStatus status);
    List<ChatRoomMember> findAllByChatRoomAndStatus(ChatRoom chatRoom, ChatMemberStatus status);
    int countByChatRoomAndStatus(ChatRoom chatRoom, ChatMemberStatus status);
    long countByChatRoomAndLastReadMessageIdLessThan(ChatRoom chatRoom, Long messageId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatRoomMember m SET m.lastReadMessageId = :messageId " +
           "WHERE m.chatRoom = :chatRoom AND m.member.id IN :memberIds AND m.status = 'JOINED'")
    void updateLastReadMessageIdByRoomAndMemberIds(@Param("chatRoom") ChatRoom chatRoom, 
                                                   @Param("memberIds") Collection<Long> memberIds, 
                                                   @Param("messageId") Long messageId);
}
