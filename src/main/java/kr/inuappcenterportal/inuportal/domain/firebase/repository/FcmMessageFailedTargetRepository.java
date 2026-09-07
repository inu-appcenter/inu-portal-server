package kr.inuappcenterportal.inuportal.domain.firebase.repository;

import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessageFailedTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FcmMessageFailedTargetRepository extends JpaRepository<FcmMessageFailedTarget, Long> {

    @Query("SELECT t.memberId FROM FcmMessageFailedTarget t WHERE t.fcmMessageId = :fcmMessageId")
    List<Long> findMemberIdsByFcmMessageId(@Param("fcmMessageId") Long fcmMessageId);

    int countByFcmMessageId(Long fcmMessageId);

    /**
     * 목록 화면에서 알림마다 재시도 대상 수를 세느라 N+1 쿼리가 나가지 않도록 한 번에 집계한다.
     * 반환은 {@code [fcmMessageId, count]} 행이며, 실패자가 없는 알림은 행 자체가 없다.
     */
    @Query("""
            SELECT t.fcmMessageId, COUNT(t)
            FROM FcmMessageFailedTarget t
            WHERE t.fcmMessageId IN :fcmMessageIds
            GROUP BY t.fcmMessageId
            """)
    List<Object[]> countGroupedByFcmMessageIds(@Param("fcmMessageIds") List<Long> fcmMessageIds);

    void deleteByFcmMessageId(Long fcmMessageId);
}
