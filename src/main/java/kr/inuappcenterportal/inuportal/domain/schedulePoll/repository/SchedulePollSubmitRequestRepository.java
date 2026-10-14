package kr.inuappcenterportal.inuportal.domain.schedulePoll.repository;

import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePollSubmitRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchedulePollSubmitRequestRepository extends JpaRepository<SchedulePollSubmitRequest, Long> {

    Optional<SchedulePollSubmitRequest> findByClientSubmissionId(String clientSubmissionId);
}
