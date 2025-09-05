package kr.inuappcenterportal.inuportal.domain.keyword.repository;

import kr.inuappcenterportal.inuportal.domain.keyword.domain.Keyword;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    @Query("SELECT DISTINCT k.memberId FROM Keyword k " +
            "WHERE :title LIKE CONCAT('%', k.keyword, '%') " +
            "AND k.department = :department")
    List<Long> findMemberIdsByKeywordAndDepartmentMatches(@Param("title") String title,
                                                          @Param("department")Department department);

    List<Keyword> findAllByMemberId(Long memberId);

    boolean existsByIdAndMemberId(Long id, Long memberId);
}
