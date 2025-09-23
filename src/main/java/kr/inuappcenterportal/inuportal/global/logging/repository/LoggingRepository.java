package kr.inuappcenterportal.inuportal.global.logging.repository;

import kr.inuappcenterportal.inuportal.global.logging.domain.Logging;
import kr.inuappcenterportal.inuportal.global.logging.dto.res.LoggingApiResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface LoggingRepository extends JpaRepository<Logging, Long> {

    @Query("""
    SELECT DISTINCT l.memberId
    FROM Logging l
    WHERE l.createDate = :createDate
    AND l.memberId IS NOT NULL
    AND l.memberId <> '-1'
    AND l.uri = '/api/members'
""")
    List<String> findDistinctMemberIdsByCreateDate(LocalDate createDate);

    @Query("""
    SELECT new kr.inuappcenterportal.inuportal.global.logging.dto.res.LoggingApiResponse(l.uri, COUNT(l.uri))
    FROM Logging l
    WHERE l.createDate = :createDate
    AND l.uri NOT IN (:excludedUris)
    GROUP BY l.uri
    ORDER BY COUNT(l.uri) DESC
""")
    List<LoggingApiResponse> findApILogsByCreateDate(LocalDate createDate, List<String> excludedUris, Pageable pageable);

    List<Logging> findAllByCreateDateBefore(LocalDate createDateBefore, Pageable pageable);
}
