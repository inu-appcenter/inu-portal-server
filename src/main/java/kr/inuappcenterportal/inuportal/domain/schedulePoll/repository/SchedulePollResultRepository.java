package kr.inuappcenterportal.inuportal.domain.schedulePoll.repository;

import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.ResultState;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePollResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchedulePollResultRepository extends JpaRepository<SchedulePollResult, Long> {

    Optional<SchedulePollResult> findByPollIdAndVersion(Long pollId, int version);

    Optional<SchedulePollResult> findByPollIdAndState(Long pollId, ResultState state);
}
