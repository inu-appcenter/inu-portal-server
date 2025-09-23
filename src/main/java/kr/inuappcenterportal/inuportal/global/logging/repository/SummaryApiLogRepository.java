package kr.inuappcenterportal.inuportal.global.logging.repository;

import kr.inuappcenterportal.inuportal.global.logging.domain.SummaryApiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface SummaryApiLogRepository extends JpaRepository<SummaryApiLog, Long> {

    @Query("""
    SELECT sal.id
    FROM SummaryApiLog sal
    WHERE sal.date = :date
""")
    Optional<Long> findIdByDate(LocalDate date);
}
