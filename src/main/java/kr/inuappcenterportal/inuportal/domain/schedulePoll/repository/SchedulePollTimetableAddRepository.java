package kr.inuappcenterportal.inuportal.domain.schedulePoll.repository;

import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePollTimetableAdd;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchedulePollTimetableAddRepository extends JpaRepository<SchedulePollTimetableAdd, Long> {

    Optional<SchedulePollTimetableAdd> findByMemberIdAndPollIdAndVersion(Long memberId, Long pollId, int version);
}
