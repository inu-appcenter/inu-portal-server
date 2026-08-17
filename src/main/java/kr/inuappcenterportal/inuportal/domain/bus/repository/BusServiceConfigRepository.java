package kr.inuappcenterportal.inuportal.domain.bus.repository;

import kr.inuappcenterportal.inuportal.domain.bus.entity.BusServiceConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusServiceConfigRepository extends JpaRepository<BusServiceConfig, Long> {
    Optional<BusServiceConfig> findByConfigKey(String configKey);
}
