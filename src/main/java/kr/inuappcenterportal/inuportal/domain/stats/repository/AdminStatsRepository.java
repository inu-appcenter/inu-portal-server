package kr.inuappcenterportal.inuportal.domain.stats.repository;

import kr.inuappcenterportal.inuportal.global.logging.domain.SummaryMemberLog;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 하루 단위 로그 요약(summary_*)과 회원 테이블에서 기간 통계를 읽는다.
 * 요약 로그의 memberId는 로그인 회원이면 숫자, 비로그인이면 IP 문자열이다.
 */
public interface AdminStatsRepository extends Repository<SummaryMemberLog, Long> {

    @Query("""
    SELECT new kr.inuappcenterportal.inuportal.domain.stats.repository.StatsRows$DailyVisit(
        s.date,
        SUM(CASE WHEN i.memberId LIKE '%.%' OR i.memberId LIKE '%:%' THEN 0 ELSE 1 END),
        SUM(CASE WHEN i.memberId LIKE '%.%' OR i.memberId LIKE '%:%' THEN 1 ELSE 0 END))
    FROM SummaryMemberLog s, SummaryMemberLogItem i
    WHERE i.summaryMemberLogId = s.id
      AND s.date BETWEEN :from AND :to
    GROUP BY s.date
""")
    List<StatsRows.DailyVisit> findDailyVisits(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
    SELECT s.date
    FROM SummaryMemberLog s
    WHERE s.date BETWEEN :from AND :to
""")
    List<LocalDate> findLoggedDates(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
    SELECT DISTINCT i.memberId
    FROM SummaryMemberLog s, SummaryMemberLogItem i
    WHERE i.summaryMemberLogId = s.id
      AND s.date BETWEEN :from AND :to
      AND i.memberId NOT LIKE '%.%'
      AND i.memberId NOT LIKE '%:%'
""")
    List<String> findDistinctLoginMemberIds(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
    SELECT new kr.inuappcenterportal.inuportal.domain.stats.repository.StatsRows$MemberJoin(m.id, m.joinedAt)
    FROM Member m
    WHERE m.joinedAt >= :start AND m.joinedAt < :end
""")
    List<StatsRows.MemberJoin> findMemberJoins(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(m) FROM Member m")
    long countMembers();

    @Query("""
    SELECT new kr.inuappcenterportal.inuportal.domain.stats.repository.StatsRows$ApiDaily(s.date, i.method, i.uri, i.apiCount)
    FROM SummaryApiLog s, SummaryApiLogItem i
    WHERE i.summaryApiLogId = s.id
      AND s.date BETWEEN :from AND :to
""")
    List<StatsRows.ApiDaily> findApiDaily(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
    SELECT new kr.inuappcenterportal.inuportal.domain.stats.repository.StatsRows$ApiFirstSeen(i.method, i.uri, MIN(s.date))
    FROM SummaryApiLog s, SummaryApiLogItem i
    WHERE i.summaryApiLogId = s.id
    GROUP BY i.method, i.uri
""")
    List<StatsRows.ApiFirstSeen> findApiFirstSeen();
}
