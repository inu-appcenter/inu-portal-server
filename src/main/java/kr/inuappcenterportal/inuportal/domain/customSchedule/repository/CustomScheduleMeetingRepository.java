package kr.inuappcenterportal.inuportal.domain.customSchedule.repository;

import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomScheduleMeeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomScheduleMeetingRepository extends JpaRepository<CustomScheduleMeeting, Long> {
    void deleteAllByCustomScheduleId(Long customScheduleId);

    List<CustomScheduleMeeting> findAllByCustomScheduleId(Long customScheduleId);
}
