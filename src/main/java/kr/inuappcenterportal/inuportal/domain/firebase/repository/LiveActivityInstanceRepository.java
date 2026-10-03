package kr.inuappcenterportal.inuportal.domain.firebase.repository;

import kr.inuappcenterportal.inuportal.domain.firebase.model.LiveActivityInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LiveActivityInstanceRepository extends JpaRepository<LiveActivityInstance, Long> {

    Optional<LiveActivityInstance> findByActivityId(String activityId);

    /** 수업이 시작돼 갱신 또는 종료가 필요할 수 있는 Activity. */
    List<LiveActivityInstance> findAllByEndedFalseAndStartAtLessThanEqual(long nowMs);

    /** 이 기기들에 떠 있는 Activity 중 주어진 수업보다 먼저 시작한 것 (다음 수업 알림이 왔을 때 정리 대상). */
    List<LiveActivityInstance> findAllByMemberIdAndEndedFalseAndFcmTokenInAndStartAtLessThan(
            Long memberId, Collection<String> fcmTokens, long startAtMs);

    @Modifying
    @Query("DELETE FROM LiveActivityInstance l WHERE l.ended = true AND l.endAt < :beforeMs")
    int deleteEndedBefore(@Param("beforeMs") long beforeMs);
}
