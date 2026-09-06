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
     * 발송 시각이 도래한 SCHEDULED 행 + {@code staleBefore}보다 오래 DISPATCHING에 머문 행의
     * id를 함께 조회한다. 엔티티 대신 id만 가져오는 이유는 lease(원자적 UPDATE) 이후 최신
     * 상태를 다시 읽어야 하므로, 여기서 로드한 엔티티는 어차피 버려지기 때문이다.
     * <p>
     * DISPATCHING 행을 포함하는 이유: lease가 커밋된 뒤 리스너의 {@code markSent}/
     * {@code markFailed}가 커밋되기 전에 서버가 죽으면 그 행은 영원히 DISPATCHING에
     * 남는다(어떤 쿼리도 SCHEDULED만 보면 다시 집지 않는다). 이 구간에서는 fcm_message가
     * 아직 만들어지지 않았으므로(만들어졌다면 이미 SENT로 넘어갔을 것이다) 재선점해도
     * 중복 발송이 아니라 단순 재시도다.
     */
    @Query("""
            SELECT s.id FROM ScheduledNotification s
            WHERE (s.status = kr.inuappcenterportal.inuportal.domain.firebase.enums.ScheduledNotificationStatus.SCHEDULED
                   AND s.scheduledAt <= :now)
               OR (s.status = kr.inuappcenterportal.inuportal.domain.firebase.enums.ScheduledNotificationStatus.DISPATCHING
                   AND s.modifiedDate <= :staleBefore)
            ORDER BY s.scheduledAt ASC
            """)
    List<Long> findDueIds(@Param("now") LocalDateTime now, @Param("staleBefore") LocalDateTime staleBefore, Pageable pageable);

    /**
     * SCHEDULED → DISPATCHING 원자적 선점. 최초 선점(SCHEDULED)뿐 아니라 stale해진
     * DISPATCHING 행의 재선점도 이 한 쿼리로 처리한다 (FcmMessageRepository의
     * leasePendingForRecovery와 동일한 패턴). 조건부 UPDATE라 스케줄러 인스턴스가 둘 이상
     * 동시에 같은 행을 집어도 정확히 하나만 1을 반환한다.
     * <p>
     * {@code modifiedDate}를 이 쿼리 안에서 직접 갱신해야 한다 — 벌크 JPQL UPDATE는 JPA
     * auditing(@LastModifiedDate) 생명주기를 타지 않으므로, 여기서 세팅하지 않으면
     * "언제 마지막으로 선점됐는가"를 판단할 기준이 사라져 staleBefore 계산이 원래
     * 예약 생성 시각을 기준으로 잘못 동작한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ScheduledNotification s
            SET s.status = kr.inuappcenterportal.inuportal.domain.firebase.enums.ScheduledNotificationStatus.DISPATCHING,
                s.modifiedDate = :now
            WHERE s.id = :id
              AND (s.status = kr.inuappcenterportal.inuportal.domain.firebase.enums.ScheduledNotificationStatus.SCHEDULED
                   OR (s.status = kr.inuappcenterportal.inuportal.domain.firebase.enums.ScheduledNotificationStatus.DISPATCHING
                       AND s.modifiedDate <= :staleBefore))
            """)
    int leaseForDispatch(@Param("id") Long id, @Param("now") LocalDateTime now, @Param("staleBefore") LocalDateTime staleBefore);

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
