package kr.inuappcenterportal.inuportal.domain.member.repository;

import kr.inuappcenterportal.inuportal.domain.member.model.FriendInviteCode;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendInviteCodeRepository extends JpaRepository<FriendInviteCode, Long> {

    Optional<FriendInviteCode> findByCodeAndRevokedAtIsNull(String code);

    Optional<FriendInviteCode> findFirstByMemberAndRevokedAtIsNullOrderByIdDesc(Member member);

    List<FriendInviteCode> findAllByMemberAndRevokedAtIsNull(Member member);

    boolean existsByCode(String code);
}
