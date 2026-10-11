package kr.inuappcenterportal.inuportal.domain.schedulePoll.repository;

import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePollSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchedulePollSubmissionRepository extends JpaRepository<SchedulePollSubmission, Long> {

    boolean existsByPollIdAndMemberId(Long pollId, Long memberId);
}
