package kr.inuappcenterportal.inuportal.domain.member.repository;

import kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus;
import kr.inuappcenterportal.inuportal.domain.member.model.Friend;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {
    /**
     * 현재 상태가 {@code from}일 때만 {@code to}로 바꾼다. 바뀐 행 수(0 또는 1)를 돌려준다.
     *
     * <p>읽고 나서 확인하는 방식은 동시 요청 둘이 모두 PENDING을 읽고 둘 다 처리할 수 있다.
     * 조건부 UPDATE는 DB 행 잠금으로 직렬화돼 한 요청만 1을 받는다.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Friend f
               set f.status = :to, f.modifiedDate = :now
             where f.id = :id
               and f.status = :from
            """)
    int updateStatusIfMatches(@Param("id") Long id,
                              @Param("from") FriendStatus from,
                              @Param("to") FriendStatus to,
                              @Param("now") LocalDateTime now);

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
