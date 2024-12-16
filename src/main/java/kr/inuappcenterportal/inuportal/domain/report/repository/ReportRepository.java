package kr.inuappcenterportal.inuportal.domain.report.repository;

import kr.inuappcenterportal.inuportal.domain.report.model.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report,Long> {

    Page<Report> findAllBy(Pageable pageable);
}
