package kr.inuappcenterportal.inuportal.domain.agent.repository;

import kr.inuappcenterportal.inuportal.domain.agent.enums.CampusWatchDomain;
import kr.inuappcenterportal.inuportal.domain.agent.enums.CampusWatchStatus;
import kr.inuappcenterportal.inuportal.domain.agent.model.CampusWatchJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CampusWatchJobRepository extends JpaRepository<CampusWatchJob, Long> {

    List<CampusWatchJob> findAllByDomainAndStatus(CampusWatchDomain domain, CampusWatchStatus status);

    List<CampusWatchJob> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<CampusWatchJob> findAllByMemberIdAndStatusOrderByCreatedAtDesc(Long memberId, CampusWatchStatus status);

    Optional<CampusWatchJob> findByMemberIdAndDomainAndTargetNameAndStatus(
            Long memberId, CampusWatchDomain domain, String targetName, CampusWatchStatus status
    );

    @Modifying
    @Query("UPDATE CampusWatchJob w SET w.status = 'EXPIRED' WHERE w.status = 'ACTIVE' AND w.expiresAt <= :now")
    int expireOldJobs(@Param("now") LocalDateTime now);
}
