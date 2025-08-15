package kr.inuappcenterportal.inuportal.domain.notice.repository;

import kr.inuappcenterportal.inuportal.domain.notice.model.DepartmentCrawlerState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentCrawlerStateRepository extends JpaRepository<DepartmentCrawlerState, Long> {

    Optional<DepartmentCrawlerState> findByDeptKey(String deptKey);
}
