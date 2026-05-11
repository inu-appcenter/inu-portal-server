package kr.inuappcenterportal.inuportal.global.logging.repository;

import kr.inuappcenterportal.inuportal.global.logging.domain.Logging;
import kr.inuappcenterportal.inuportal.global.logging.dto.res.LoggingApiResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface LoggingRepository extends JpaRepository<Logging, Long> {

    @Query("""
    SELECT DISTINCT l.memberId
    FROM Logging l
    WHERE l.createDate BETWEEN :start AND :end
    AND l.memberId IS NOT NULL
    AND l.uri = '/api/members/no-dup'
""")
    List<String> findDistinctMemberIdsByCreateDate(LocalDateTime start, LocalDateTime end);

    @Query("""
    SELECT new kr.inuappcenterportal.inuportal.global.logging.dto.res.LoggingApiResponse(l.httpMethod, l.uri, COUNT(l.uri))
    FROM Logging l
    WHERE l.createDate BETWEEN :start AND :end
    AND l.uri NOT IN (:excludedUris)
    GROUP BY l.httpMethod, l.uri
    ORDER BY COUNT(1) DESC
""")
    List<LoggingApiResponse> findApILogsByCreateDate(LocalDateTime start, LocalDateTime end, List<String> excludedUris, Pageable pageable);

    List<Logging> findAllByCreateDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}
