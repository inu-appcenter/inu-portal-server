package kr.inuappcenterportal.inuportal.domain.directory.repository;

import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectoryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DirectoryEntryRepository extends JpaRepository<DirectoryEntry, Long> {

    Page<DirectoryEntry> findAllByCategory(DirectoryCategory category, Pageable pageable);

    @Query("""
            select d from DirectoryEntry d
            where lower(d.affiliation) like lower(concat('%', :query, '%'))
               or lower(d.detailAffiliation) like lower(concat('%', :query, '%'))
               or lower(coalesce(d.name, '')) like lower(concat('%', :query, '%'))
               or lower(d.position) like lower(concat('%', :query, '%'))
               or lower(coalesce(d.duties, '')) like lower(concat('%', :query, '%'))
               or lower(coalesce(d.email, '')) like lower(concat('%', :query, '%'))
               or (:normalizedQuery <> '' and coalesce(d.phoneNumberNormalized, '') like concat('%', :normalizedQuery, '%'))
            """)
    Page<DirectoryEntry> searchAll(
            @Param("query") String query,
            @Param("normalizedQuery") String normalizedQuery,
            Pageable pageable
    );

    @Query("""
            select d from DirectoryEntry d
            where d.category = :category
              and (
                    lower(d.affiliation) like lower(concat('%', :query, '%'))
                 or lower(d.detailAffiliation) like lower(concat('%', :query, '%'))
                 or lower(coalesce(d.name, '')) like lower(concat('%', :query, '%'))
                 or lower(d.position) like lower(concat('%', :query, '%'))
                 or lower(coalesce(d.duties, '')) like lower(concat('%', :query, '%'))
                 or lower(coalesce(d.email, '')) like lower(concat('%', :query, '%'))
                 or (:normalizedQuery <> '' and coalesce(d.phoneNumberNormalized, '') like concat('%', :normalizedQuery, '%'))
              )
            """)
    Page<DirectoryEntry> searchAllByCategory(
            @Param("category") DirectoryCategory category,
            @Param("query") String query,
            @Param("normalizedQuery") String normalizedQuery,
            Pageable pageable
    );

    long countByCategory(DirectoryCategory category);

    long deleteByCategory(DirectoryCategory category);
}
