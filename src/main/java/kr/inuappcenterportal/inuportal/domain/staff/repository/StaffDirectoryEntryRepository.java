package kr.inuappcenterportal.inuportal.domain.staff.repository;

import kr.inuappcenterportal.inuportal.domain.staff.enums.StaffDirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.staff.model.StaffDirectoryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffDirectoryEntryRepository extends JpaRepository<StaffDirectoryEntry, Long> {

    Page<StaffDirectoryEntry> findAllByCategory(StaffDirectoryCategory category, Pageable pageable);

    long countByCategory(StaffDirectoryCategory category);

    long deleteByCategory(StaffDirectoryCategory category);
}
