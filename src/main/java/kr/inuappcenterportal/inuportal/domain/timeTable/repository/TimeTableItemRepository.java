package kr.inuappcenterportal.inuportal.domain.timeTable.repository;

import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTableItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimeTableItemRepository extends JpaRepository<TimeTableItem, Long> {
    Optional<TimeTableItem> findByCustomScheduleId(Long customScheduleId);

    List<TimeTableItem> findAllByTimeTableId(Long timeTableId);
}
