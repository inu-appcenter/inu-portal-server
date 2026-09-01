package kr.inuappcenterportal.inuportal.domain.bus.repository;

import kr.inuappcenterportal.inuportal.domain.bus.entity.BusRouteSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusRouteSectionRepository extends JpaRepository<BusRouteSection, Long> {
    List<BusRouteSection> findByCategory(String category);
    List<BusRouteSection> findByCategoryAndTabName(String category, String tabName);
    List<BusRouteSection> findByStartBstopId(String startBstopId);
    Optional<BusRouteSection> findByRouteNoAndCategoryAndTabName(String routeNo, String category, String tabName);
    Optional<BusRouteSection> findByRouteNoAndCategoryAndTabNameAndStartBstopId(String routeNo, String category, String tabName, String startBstopId);
}
