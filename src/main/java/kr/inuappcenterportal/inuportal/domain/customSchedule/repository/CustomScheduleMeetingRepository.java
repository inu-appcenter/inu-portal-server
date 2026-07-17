package kr.inuappcenterportal.inuportal.domain.customSchedule.repository;

import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomScheduleMeeting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomScheduleMeetingRepository extends JpaRepository<CustomScheduleMeeting, Long> {
    void deleteAllByCustomScheduleId(Long customScheduleId);

    Long countByCustomScheduleId(Long customScheduleId);
}
