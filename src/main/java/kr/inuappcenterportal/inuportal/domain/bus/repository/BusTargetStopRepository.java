package kr.inuappcenterportal.inuportal.domain.bus.repository;

import kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusTargetStopRepository extends JpaRepository<BusTargetStop, Long> {
    List<BusTargetStop> findByIsActiveTrue();
    Optional<BusTargetStop> findByBstopId(String bstopId);
    boolean existsByBstopId(String bstopId);

}
