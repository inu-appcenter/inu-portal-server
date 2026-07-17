package kr.inuappcenterportal.inuportal.domain.customSchedule.repository;

import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomScheduleRepository extends JpaRepository<CustomSchedule, Long> {
}
