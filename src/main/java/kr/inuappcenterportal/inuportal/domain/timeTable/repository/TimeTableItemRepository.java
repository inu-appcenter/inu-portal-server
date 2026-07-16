package kr.inuappcenterportal.inuportal.domain.timeTable.repository;

import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTableItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeTableItemRepository extends JpaRepository<TimeTableItem, Long> {
}
