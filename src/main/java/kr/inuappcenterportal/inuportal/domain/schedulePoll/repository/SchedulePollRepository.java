package kr.inuappcenterportal.inuportal.domain.schedulePoll.repository;

import jakarta.persistence.LockModeType;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePoll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SchedulePollRepository extends JpaRepository<SchedulePoll, Long> {

    // 데이터를 바꾸는 요청은 모두 이 잠금부터 잡는다. 잠금 순서: 조율 → 참여자 → 제출본
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM SchedulePoll p WHERE p.id = :id")
    Optional<SchedulePoll> findByIdForUpdate(@Param("id") Long id);
}
