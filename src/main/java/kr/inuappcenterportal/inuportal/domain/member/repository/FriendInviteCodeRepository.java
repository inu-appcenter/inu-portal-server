package kr.inuappcenterportal.inuportal.domain.member.repository;

import kr.inuappcenterportal.inuportal.domain.member.model.FriendInviteCode;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendInviteCodeRepository extends JpaRepository<FriendInviteCode, Long> {

    Optional<FriendInviteCode> findByCodeAndRevokedAtIsNull(String code);

    Optional<FriendInviteCode> findFirstByMemberAndRevokedAtIsNullOrderByIdDesc(Member member);

    List<FriendInviteCode> findAllByMemberAndRevokedAtIsNull(Member member);

    boolean existsByCode(String code);

    // 회원탈퇴 시 해당 회원의 데이터를 한 번에 지운다.
    @Modifying(flushAutomatically = true)
    @Query("delete from FriendInviteCode c where c.member.id = :memberId")
    int deleteAllByMemberIdForWithdrawal(@Param("memberId") Long memberId);
}
