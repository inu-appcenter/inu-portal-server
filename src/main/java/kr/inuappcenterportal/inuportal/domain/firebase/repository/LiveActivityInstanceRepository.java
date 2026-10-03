package kr.inuappcenterportal.inuportal.domain.firebase.repository;

import kr.inuappcenterportal.inuportal.domain.firebase.model.LiveActivityInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LiveActivityInstanceRepository extends JpaRepository<LiveActivityInstance, Long> {

    Optional<LiveActivityInstance> findByActivityId(String activityId);

    /** 수업이 시작돼 갱신 또는 종료가 필요할 수 있는 Activity. */
    List<LiveActivityInstance> findAllByEndedFalseAndStartAtLessThanEqual(long nowMs);

    @Modifying
    @Query("DELETE FROM LiveActivityInstance l WHERE l.ended = true AND l.endAt < :beforeMs")
    int deleteEndedBefore(@Param("beforeMs") long beforeMs);
}
