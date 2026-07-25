package kr.inuappcenterportal.inuportal.domain.member.repository;

import io.lettuce.core.dynamic.annotation.Param;
import kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus;
import kr.inuappcenterportal.inuportal.domain.member.model.Friend;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {
    Optional<Friend> findByRequesterAndReceiver(Member requester, Member receiver);

    List<Friend> findAllByRequesterAndStatus(Member requester, FriendStatus status);

    List<Friend> findAllByReceiverAndStatus(Member receiver, FriendStatus status);

    boolean existsByRequesterAndReceiverAndStatus(Member requester, Member receiver, FriendStatus status);

    boolean existsByRequesterAndReceiver(Member requester, Member receiver);

    @Query("""
            select case when count(f) > 0 then true else false end
            from Friend f
            where f.status = :status
              and (
                (f.requester.id = :memberId and f.receiver.id = :targetMemberId)
                or
                (f.requester.id = :targetMemberId and f.receiver.id = :memberId)
              )
            """)
    boolean existsFriendship(
            @Param("memberId") Long memberId,
            @Param("targetMemberId") Long targetMemberId,
            @Param("status") FriendStatus status
    );
}
