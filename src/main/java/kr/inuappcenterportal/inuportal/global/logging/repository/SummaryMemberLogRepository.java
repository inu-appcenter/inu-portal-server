package kr.inuappcenterportal.inuportal.global.logging.repository;

import kr.inuappcenterportal.inuportal.global.logging.domain.SummaryMemberLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SummaryMemberLogRepository extends JpaRepository<SummaryMemberLog, Long> {

    Optional<SummaryMemberLog> findByDate(LocalDate date);
}
