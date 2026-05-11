package kr.inuappcenterportal.inuportal.domain.member.repository;

import kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus;
import kr.inuappcenterportal.inuportal.domain.member.model.Friend;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {
    Optional<Friend> findByRequesterAndReceiver(Member requester, Member receiver);
    List<Friend> findAllByRequesterAndStatus(Member requester, FriendStatus status);
    List<Friend> findAllByReceiverAndStatus(Member receiver, FriendStatus status);
    boolean existsByRequesterAndReceiverAndStatus(Member requester, Member receiver, FriendStatus status);
    boolean existsByRequesterAndReceiver(Member requester, Member receiver);
}
