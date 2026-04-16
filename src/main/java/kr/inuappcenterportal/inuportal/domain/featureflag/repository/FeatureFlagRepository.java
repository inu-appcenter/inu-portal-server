package kr.inuappcenterportal.inuportal.domain.featureflag.repository;

import kr.inuappcenterportal.inuportal.domain.featureflag.model.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {
    Optional<FeatureFlag> findByFlagKey(String flagKey);
    boolean existsByFlagKey(String flagKey);
    List<FeatureFlag> findAllByClientVisibleTrue();
}
