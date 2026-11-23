package kr.inuappcenterportal.inuportal.domain.member.repository;

import kr.inuappcenterportal.inuportal.domain.member.model.Block;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {
    Optional<Block> findByBlockerAndBlocked(Member blocker, Member blocked);
    List<Block> findAllByBlocker(Member blocker);
    boolean existsByBlockerAndBlocked(Member blocker, Member blocked);

    // 회원탈퇴 시 해당 회원의 데이터를 한 번에 지운다.
    @Modifying(flushAutomatically = true)
    @Query("delete from Block b where b.blocker.id = :memberId or b.blocked.id = :memberId")
    int deleteAllByMemberIdForWithdrawal(@Param("memberId") Long memberId);
}
