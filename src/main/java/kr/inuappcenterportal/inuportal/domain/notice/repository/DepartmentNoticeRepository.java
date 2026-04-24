package kr.inuappcenterportal.inuportal.domain.notice.repository;

import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import kr.inuappcenterportal.inuportal.domain.notice.enums.DepartmentNoticeContentStatus;
import kr.inuappcenterportal.inuportal.domain.notice.enums.DepartmentNoticeScheduleExtractStatus;
import kr.inuappcenterportal.inuportal.domain.notice.model.DepartmentNotice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DepartmentNoticeRepository extends JpaRepository<DepartmentNotice, Long> {

    Page<DepartmentNotice> findAllByDepartment(Department department, Pageable pageable);

    Optional<DepartmentNotice> findFirstByDepartmentAndUrl(Department department, String url);

    Optional<DepartmentNotice> findFirstByDepartmentAndTitleAndCreateDate(Department department, String title, LocalDate createDate);

    @Query("""
            select dn from DepartmentNotice dn
            join fetch dn.content
            where dn.department = :department
              and (
                    dn.content.contentText is null
                    or dn.content.inlineImageUrlsJson is null
                    or dn.content.attachmentMetaJson is null
              )
              and (dn.contentStatus is null or dn.contentStatus in :statuses)
            order by dn.id desc
            """)
    List<DepartmentNotice> findBackfillTargetsByDepartment(
            Department department,
            List<DepartmentNoticeContentStatus> statuses,
            Pageable pageable
    );

    List<DepartmentNotice> findByDepartmentAndContentStatusInOrderByIdDesc(
            Department department,
            List<DepartmentNoticeContentStatus> statuses,
            Pageable pageable
    );

    @Query("""
            select dn from DepartmentNotice dn
            join fetch dn.content
            where dn.contentStatus = :contentStatus
              and (dn.scheduleExtractStatus is null or dn.scheduleExtractStatus in :statuses)
            order by dn.id desc
            """)
    List<DepartmentNotice> findScheduleExtractTargets(
            DepartmentNoticeContentStatus contentStatus,
            List<DepartmentNoticeScheduleExtractStatus> statuses,
            Pageable pageable
    );
}
