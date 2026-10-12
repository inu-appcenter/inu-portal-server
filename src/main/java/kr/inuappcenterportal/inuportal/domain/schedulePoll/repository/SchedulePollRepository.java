package kr.inuappcenterportal.inuportal.domain.schedulePoll.repository;

import jakarta.persistence.LockModeType;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.SchedulePollStatus;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePoll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SchedulePollRepository extends JpaRepository<SchedulePoll, Long> {

    // 데이터를 바꾸는 요청은 모두 이 잠금부터 잡는다. 잠금 순서: 조율 → 참여자 → 제출본
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM SchedulePoll p WHERE p.id = :id")
    Optional<SchedulePoll> findByIdForUpdate(@Param("id") Long id);

    Optional<SchedulePoll> findByLinkToken(String linkToken);

    boolean existsByLinkToken(String linkToken);

    // 마감 스케줄러용. 마감 시각이 지났는데 아직 끝나지 않은 조율
    @Query("SELECT p.id FROM SchedulePoll p WHERE p.status <> :closed AND p.deadlineAt <= :now ORDER BY p.deadlineAt ASC")
    List<Long> findDueIds(@Param("closed") SchedulePollStatus closed, @Param("now") LocalDateTime now);

    // 마감 1시간 전 알림용. 마감 전 1시간 안에 들어왔고 아직 알림을 보내지 않은 조율
    @Query("""
            SELECT p.id FROM SchedulePoll p
            WHERE p.status <> :closed AND p.deadlineRemindedAt IS NULL
              AND p.deadlineAt > :now AND p.deadlineAt <= :until
            """)
    List<Long> findReminderDueIds(@Param("closed") SchedulePollStatus closed,
                                  @Param("now") LocalDateTime now, @Param("until") LocalDateTime until);

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
