package kr.inuappcenterportal.inuportal.domain.schedulePoll.repository;

import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePollSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SchedulePollSubmissionRepository extends JpaRepository<SchedulePollSubmission, Long> {

    boolean existsByPollIdAndMemberId(Long pollId, Long memberId);

    Optional<SchedulePollSubmission> findByPollIdAndMemberId(Long pollId, Long memberId);

    // 내 조율 목록용. 주어진 조율 중 내가 제출한 조율의 ID
    @Query("SELECT s.poll.id FROM SchedulePollSubmission s WHERE s.member.id = :memberId AND s.poll.id IN :pollIds")
    List<Long> findSubmittedPollIds(@Param("memberId") Long memberId, @Param("pollIds") Collection<Long> pollIds);

    // 참여 순서(처음 제출 순). 이 순서가 히트맵 자리(index)다.
    @Query("""
            SELECT s FROM SchedulePollSubmission s JOIN FETCH s.member
            WHERE s.poll.id = :pollId
            ORDER BY s.joinedAt ASC, s.id ASC
            """)
    List<SchedulePollSubmission> findAllInJoinOrder(@Param("pollId") Long pollId);
}
