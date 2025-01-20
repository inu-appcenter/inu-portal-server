package kr.inuappcenterportal.inuportal.domain.lostProperty.repository;

import kr.inuappcenterportal.inuportal.domain.lostProperty.model.LostProperty;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LostPropertyRepository extends JpaRepository<LostProperty, Long> {
}
