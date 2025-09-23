package kr.inuappcenterportal.inuportal.global.logging.repository;

import kr.inuappcenterportal.inuportal.global.logging.domain.SummaryApiLogItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SummaryApiLogItemRepository extends JpaRepository<SummaryApiLogItem, Long> {

    List<SummaryApiLogItem> findAllBySummaryApiLogId(Long summaryApiLogId);
}
