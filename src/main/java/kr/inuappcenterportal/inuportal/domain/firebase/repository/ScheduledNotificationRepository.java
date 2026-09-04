package kr.inuappcenterportal.inuportal.domain.firebase.repository;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.ScheduledNotificationStatus;
import kr.inuappcenterportal.inuportal.domain.firebase.model.ScheduledNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledNotificationRepository extends JpaRepository<ScheduledNotification, Long> {

    Page<ScheduledNotification> findAllByOrderByScheduledAtDesc(Pageable pageable);

    Page<ScheduledNotification> findAllByStatusOrderByScheduledAtDesc(ScheduledNotificationStatus status, Pageable pageable);

    /**
     * 발송 시각이 도래한 SCHEDULED 행의 id만 조회한다. 엔티티 대신 id만 가져오는 이유는
     * lease(원자적 UPDATE) 이후 최신 상태를 다시 읽어야 하므로, 여기서 로드한 엔티티는
     * 어차피 버려지기 때문이다.
     */
    @Query("""
            SELECT s.id FROM ScheduledNotification s
            WHERE s.status = kr.inuappcenterportal.inuportal.domain.firebase.enums.ScheduledNotificationStatus.SCHEDULED
              AND s.scheduledAt <= :now
            ORDER BY s.scheduledAt ASC
            """)
    List<Long> findDueIds(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * SCHEDULED → DISPATCHING 원자적 선점. 조건부 UPDATE라 스케줄러 인스턴스가 둘 이상
     * 동시에 같은 행을 집어도 정확히 하나만 1을 반환한다 (FcmMessageRepository의
     * leasePendingForRecovery와 동일한 패턴).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ScheduledNotification s
            SET s.status = kr.inuappcenterportal.inuportal.domain.firebase.enums.ScheduledNotificationStatus.DISPATCHING
            WHERE s.id = :id
              AND s.status = kr.inuappcenterportal.inuportal.domain.firebase.enums.ScheduledNotificationStatus.SCHEDULED
            """)
    int leaseForDispatch(@Param("id") Long id);

    /**
     * 취소는 SCHEDULED 상태에서만 허용한다. 이미 DISPATCHING으로 넘어간(=선점되어 발송
     * 이벤트가 발행된) 행을 취소하면 발송은 막지 못한 채 상태만 어긋나므로, 조건부 UPDATE로
     * 경합을 막는다. 반환값이 0이면 이미 발송 절차가 시작됐다는 뜻이다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ScheduledNotification s
            SET s.status = kr.inuappcenterportal.inuportal.domain.firebase.enums.ScheduledNotificationStatus.CANCELED
            WHERE s.id = :id
              AND s.status = kr.inuappcenterportal.inuportal.domain.firebase.enums.ScheduledNotificationStatus.SCHEDULED
            """)
    int cancelIfScheduled(@Param("id") Long id);
}
