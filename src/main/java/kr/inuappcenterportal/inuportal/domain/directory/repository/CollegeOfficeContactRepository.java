package kr.inuappcenterportal.inuportal.domain.directory.repository;

import kr.inuappcenterportal.inuportal.domain.directory.model.CollegeOfficeContact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollegeOfficeContactRepository extends JpaRepository<CollegeOfficeContact, Long> {

    Page<CollegeOfficeContact> findAllByCollegeName(String collegeName, Pageable pageable);

    @Query("""
            select c from CollegeOfficeContact c
            where lower(c.collegeName) like lower(concat('%', :query, '%'))
               or lower(c.departmentName) like lower(concat('%', :query, '%'))
               or lower(coalesce(c.collegeLocationSummary, '')) like lower(concat('%', :query, '%'))
               or lower(coalesce(c.officeLocation, '')) like lower(concat('%', :query, '%'))
               or lower(coalesce(c.homepageUrl, '')) like lower(concat('%', :query, '%'))
               or (:normalizedQuery <> '' and coalesce(c.officePhoneNumberNormalized, '') like concat('%', :normalizedQuery, '%'))
            """)
    Page<CollegeOfficeContact> searchAll(
            @Param("query") String query,
            @Param("normalizedQuery") String normalizedQuery,
            Pageable pageable
    );

    @Query("""
            select c from CollegeOfficeContact c
            where c.collegeName = :collegeName
              and (
                    lower(c.departmentName) like lower(concat('%', :query, '%'))
                 or lower(c.collegeName) like lower(concat('%', :query, '%'))
                 or lower(coalesce(c.collegeLocationSummary, '')) like lower(concat('%', :query, '%'))
                 or lower(coalesce(c.officeLocation, '')) like lower(concat('%', :query, '%'))
                 or lower(coalesce(c.homepageUrl, '')) like lower(concat('%', :query, '%'))
                 or (:normalizedQuery <> '' and coalesce(c.officePhoneNumberNormalized, '') like concat('%', :normalizedQuery, '%'))
              )
            """)
    Page<CollegeOfficeContact> searchAllByCollegeName(
            @Param("collegeName") String collegeName,
            @Param("query") String query,
            @Param("normalizedQuery") String normalizedQuery,
            Pageable pageable
    );

    void deleteAllInBatch();
}
