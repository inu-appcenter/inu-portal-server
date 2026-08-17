package kr.inuappcenterportal.inuportal.domain.bus.repository;

import kr.inuappcenterportal.inuportal.domain.bus.entity.BusArrivalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BusArrivalHistoryRepository extends JpaRepository<BusArrivalHistory, Long> {

    List<BusArrivalHistory> findByBstopIdAndCreateDateBetweenOrderByCreateDateAsc(
            String bstopId, LocalDateTime start, LocalDateTime end);

    List<BusArrivalHistory> findByBstopIdAndRouteNoAndCreateDateBetweenOrderByCreateDateAsc(
            String bstopId, String routeNo, LocalDateTime start, LocalDateTime end);

    List<BusArrivalHistory> findByBstopIdAndRouteIdAndCreateDateBetweenOrderByCreateDateAsc(
            String bstopId, String routeId, LocalDateTime start, LocalDateTime end);


    boolean existsByBstopIdAndRouteIdAndBusNumPlateAndCreateDateAfter(
            String bstopId, String routeId, String busNumPlate, LocalDateTime after);

    @Modifying
    @Query("DELETE FROM BusArrivalHistory b WHERE b.createDate < :cutoffDate")
    int deleteByCreateDateBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}
