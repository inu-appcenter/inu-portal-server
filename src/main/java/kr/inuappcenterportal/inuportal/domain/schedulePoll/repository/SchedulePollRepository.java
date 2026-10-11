package kr.inuappcenterportal.inuportal.domain.schedulePoll.repository;

import jakarta.persistence.LockModeType;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.SchedulePollStatus;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePoll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SchedulePollRepository extends JpaRepository<SchedulePoll, Long> {

    // 데이터를 바꾸는 요청은 모두 이 잠금부터 잡는다. 잠금 순서: 조율 → 참여자 → 제출본
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM SchedulePoll p WHERE p.id = :id")
    Optional<SchedulePoll> findByIdForUpdate(@Param("id") Long id);

    Optional<SchedulePoll> findByLinkToken(String linkToken);

    boolean existsByLinkToken(String linkToken);

    // 폴링용. 조율 정보와 내 제출 여부를 쿼리 한 번으로 가져온다.
    @Query("""
            SELECT p.revision AS revision, p.status AS status, p.resultVersion AS resultVersion,
                   p.participantCount AS participantCount, p.deadlineAt AS deadlineAt,
                   (SELECT COUNT(s) FROM SchedulePollSubmission s WHERE s.poll.id = p.id AND s.member.id = :memberId) AS submittedCount
            FROM SchedulePoll p WHERE p.id = :pollId
            """)
    Optional<RevisionView> findRevisionView(@Param("pollId") Long pollId, @Param("memberId") Long memberId);

    interface RevisionView {
        long getRevision();

        SchedulePollStatus getStatus();

        int getResultVersion();

        int getParticipantCount();

        LocalDateTime getDeadlineAt();

        long getSubmittedCount();
    }
}
