package kr.inuappcenterportal.inuportal.domain.department.repository;

import kr.inuappcenterportal.inuportal.domain.department.model.SchoolDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolDepartmentRepository extends JpaRepository<SchoolDepartment, Long> {
    Optional<SchoolDepartment> findByCode(String code);
    Optional<SchoolDepartment> findByCodeAndActiveTrue(String code);
    List<SchoolDepartment> findAllByActiveTrueOrderByNameAscCodeAsc();
}
