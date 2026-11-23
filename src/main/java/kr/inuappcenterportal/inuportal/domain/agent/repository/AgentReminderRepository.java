package kr.inuappcenterportal.inuportal.domain.agent.repository;

import kr.inuappcenterportal.inuportal.domain.agent.model.AgentReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface AgentReminderRepository extends JpaRepository<AgentReminder, Long> {

    List<AgentReminder> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

    Optional<AgentReminder> findByIdAndMemberId(Long id, Long memberId);

    @Query("SELECT r FROM AgentReminder r JOIN FETCH r.member WHERE r.targetTime = :targetTime AND r.enabled = true")
    List<AgentReminder> findAllActiveByTargetTime(@Param("targetTime") String targetTime);

    @Query("SELECT r FROM AgentReminder r JOIN FETCH r.member WHERE r.enabled = true")
    List<AgentReminder> findAllActive();

    // 회원탈퇴 시 해당 회원의 데이터를 한 번에 지운다.
    @Modifying(flushAutomatically = true)
    @Query("delete from AgentReminder a where a.member.id = :memberId")
    int deleteAllByMemberIdForWithdrawal(@Param("memberId") Long memberId);
}
