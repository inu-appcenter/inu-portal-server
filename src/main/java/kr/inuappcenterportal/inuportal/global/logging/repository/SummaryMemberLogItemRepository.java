package kr.inuappcenterportal.inuportal.global.logging.repository;

import kr.inuappcenterportal.inuportal.global.logging.domain.SummaryMemberLogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SummaryMemberLogItemRepository extends JpaRepository<SummaryMemberLogItem, Long> {

    @Query("""
    SELECT smli.memberId
    FROM SummaryMemberLogItem smli
    WHERE smli.summaryMemberLogId = :summaryMemberLogId
""")
    List<String> findAllBySummaryMemberLogId(Long summaryMemberLogId);
}
