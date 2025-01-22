package kr.inuappcenterportal.inuportal.domain.report.repository;

import kr.inuappcenterportal.inuportal.domain.report.model.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report,Long> {

    Page<Report> findAllBy(Pageable pageable);
    boolean existsByPostIdAndMemberId(Long postId, Long memberId);
    @Query("SELECT r.postId FROM Report r WHERE r.memberId = :memberId")
    List<Long> findPostIdsByMemberId(@Param("memberId") Long memberId);
}
