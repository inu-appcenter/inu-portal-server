package kr.inuappcenterportal.inuportal.domain.timeTable.repository;

import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTableItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TimeTableItemRepository extends JpaRepository<TimeTableItem, Long> {
    void deleteAllByTimeTableId(Long timeTableId);

    Optional<TimeTableItem> findByCustomScheduleId(Long customScheduleId);
}
