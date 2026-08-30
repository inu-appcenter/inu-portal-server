package kr.inuappcenterportal.inuportal.domain.firebase.repository;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmSendStatus;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FcmMessageRepository extends JpaRepository<FcmMessage, Long> {

    Page<FcmMessage> findAllByAdminMessageTrue(Pageable pageable);

    Optional<FcmMessage> findByIdAndAdminMessageTrue(Long id);

    /**
     * 유실 보정 스케줄러의 재발행 후보를 찾는다. 반드시 아래 두 하한/상한을 함께 걸어야 한다.
     * <p>
     * - {@code notBefore} (하한): 이 시각 이전에 생성된 행은 절대 건드리지 않는다. send_status
     * 컬럼 도입 이전 레거시 backfill 잔재가 재처리 대상으로 오인되는 사고(#431)를 구조적으로
     * 차단하는 경계다. 값은 컬럼 backfill을 정리한 마이그레이션의 CUTOFF와 같아야 한다.
     * <p>
     * - {@code stalledBefore} (상한): 이 시각 이후에 수정된(=최근 활동이 있는) 행은 아직
     * 정상 처리 중일 수 있으므로 건드리지 않는다.
     */
    @Query("""
            SELECT f FROM FcmMessage f
            WHERE f.sendStatus = :status
              AND f.createDate >= :notBefore
              AND f.modifiedDate < :stalledBefore
            ORDER BY f.id ASC
            """)
    List<FcmMessage> findStalledCandidates(
            @Param("status") FcmSendStatus status,
            @Param("notBefore") LocalDateTime notBefore,
            @Param("stalledBefore") LocalDateTime stalledBefore,
            Pageable pageable
    );

    /**
     * PENDING → PROCESSING 원자적 선점(lease). WHERE 절에 현재 상태 조건을 포함하는
     * 조건부 UPDATE라 두 스케줄러 인스턴스(혹은 스케줄러와 뒤늦게 실행된 AFTER_COMMIT
     * 리스너)가 동시에 같은 행을 집어도 정확히 하나만 1을 반환한다. 이 값이 1일 때만
     * 재발행 이벤트를 publish해야 중복 발송을 피할 수 있다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE FcmMessage f
            SET f.sendStatus = kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmSendStatus.PROCESSING
            WHERE f.id = :id
              AND f.sendStatus = kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmSendStatus.PENDING
            """)
    int leasePendingForRecovery(@Param("id") Long id);

    /**
     * notBefore 이후 생성됐지만 maxAgeBefore보다도 오래된 PENDING을 일괄 ABANDONED로
     * 종결한다. 알림은 시의성이 본질이라, 이 시점까지 발송을 시작조차 못 했다면 재발행보다
     * 포기가 낫다. 대량 UPDATE라 엔티티 로딩 없이 벌크 쿼리로 처리한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE FcmMessage f
            SET f.sendStatus = kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmSendStatus.ABANDONED
            WHERE f.sendStatus = kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmSendStatus.PENDING
              AND f.createDate >= :notBefore
              AND f.createDate < :maxAgeBefore
            """)
    int abandonPendingOlderThan(@Param("notBefore") LocalDateTime notBefore, @Param("maxAgeBefore") LocalDateTime maxAgeBefore);
}
