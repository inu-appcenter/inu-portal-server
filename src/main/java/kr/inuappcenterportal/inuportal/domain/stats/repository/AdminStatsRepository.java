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
 *
 * 요약 로그의 memberId 분류 (ServerStatsCollector와 같은 규칙):
 * - 회원: 실제 존재하는 회원 ID
 * - 비회원: IP 형태('.' 또는 ':' 포함)
 * - 그 외('-1' 등): 어느 쪽에도 세지 않음
 *
 * 같은 날짜의 요약이 여러 번 저장된 경우(날짜에 유일 제약이 없음) id가 가장 작은 요약 하나만 읽는다.
 */
public interface AdminStatsRepository extends Repository<SummaryMemberLog, Long> {

    @Query("""
    SELECT new kr.inuappcenterportal.inuportal.domain.stats.repository.StatsRows$DailyCount(s.date, COUNT(i))
    FROM SummaryMemberLog s, SummaryMemberLogItem i, Member m
    WHERE i.summaryMemberLogId = s.id
      AND s.id = (SELECT MIN(s2.id) FROM SummaryMemberLog s2 WHERE s2.date = s.date)
      AND s.date BETWEEN :from AND :to
      AND i.memberId = CAST(m.id AS String)
    GROUP BY s.date
""")
    List<StatsRows.DailyCount> countDailyMembers(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
    SELECT new kr.inuappcenterportal.inuportal.domain.stats.repository.StatsRows$DailyCount(s.date, COUNT(i))
    FROM SummaryMemberLog s, SummaryMemberLogItem i
    WHERE i.summaryMemberLogId = s.id
      AND s.id = (SELECT MIN(s2.id) FROM SummaryMemberLog s2 WHERE s2.date = s.date)
      AND s.date BETWEEN :from AND :to
      AND (i.memberId LIKE '%.%' OR i.memberId LIKE '%:%')
    GROUP BY s.date
""")
    List<StatsRows.DailyCount> countDailyGuests(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
    SELECT DISTINCT s.date
    FROM SummaryMemberLog s
    WHERE s.date BETWEEN :from AND :to
""")
    List<LocalDate> findSummarizedDates(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
    SELECT DISTINCT i.memberId
    FROM SummaryMemberLog s, SummaryMemberLogItem i, Member m
    WHERE i.summaryMemberLogId = s.id
      AND s.date BETWEEN :from AND :to
      AND i.memberId = CAST(m.id AS String)
""")
    List<String> findDistinctMemberIds(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT m.id FROM Member m")
    List<Long> findAllMemberIds();

    @Query("""
    SELECT new kr.inuappcenterportal.inuportal.domain.stats.repository.StatsRows$MemberJoin(m.id, m.joinedAt)
    FROM Member m
    WHERE m.joinedAt >= :start AND m.joinedAt < :end
""")
    List<StatsRows.MemberJoin> findMemberJoins(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
    SELECT new kr.inuappcenterportal.inuportal.domain.stats.repository.StatsRows$ApiDaily(s.date, i.method, i.uri, i.apiCount)
    FROM SummaryApiLog s, SummaryApiLogItem i
    WHERE i.summaryApiLogId = s.id
      AND s.id = (SELECT MIN(s2.id) FROM SummaryApiLog s2 WHERE s2.date = s.date)
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
