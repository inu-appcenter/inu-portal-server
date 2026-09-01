package kr.inuappcenterportal.inuportal.domain.bus.repository;

import kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusTargetRuleRepository extends JpaRepository<BusTargetRule, Long> {

    List<BusTargetRule> findByCategory(String category);
}
