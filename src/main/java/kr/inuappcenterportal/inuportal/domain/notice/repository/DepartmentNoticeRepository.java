package kr.inuappcenterportal.inuportal.domain.notice.repository;

import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import kr.inuappcenterportal.inuportal.domain.notice.model.DepartmentNotice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentNoticeRepository extends JpaRepository<DepartmentNotice, Long> {

    Page<DepartmentNotice> findAllByDepartment(Department department, Pageable pageable);

    Optional<DepartmentNotice> findFirstByDepartmentAndTitleAndCreateDate(Department department, String title, String createDate);
}
