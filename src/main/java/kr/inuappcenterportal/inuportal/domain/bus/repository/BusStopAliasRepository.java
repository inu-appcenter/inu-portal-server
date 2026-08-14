package kr.inuappcenterportal.inuportal.domain.bus.repository;

import kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusStopAliasRepository extends JpaRepository<BusStopAlias, Long> {
    Optional<BusStopAlias> findByBstopId(String bstopId);
    boolean existsByBstopId(String bstopId);
}
