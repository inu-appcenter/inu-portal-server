package kr.inuappcenterportal.inuportal.domain.stats.service;

import kr.inuappcenterportal.inuportal.domain.stats.dto.AdminStatsResponse;
import kr.inuappcenterportal.inuportal.domain.stats.dto.ServerStats;
import kr.inuappcenterportal.inuportal.domain.stats.dto.SourceResult;
import kr.inuappcenterportal.inuportal.domain.stats.dto.StatsQuery;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 관리자 통계를 데이터 출처별로 모은다.
 * 출처마다 SourceResult로 감싸서, 한 출처가 실패해도 나머지 결과는 응답한다.
 * Mixpanel 연동은 MixpanelStatsCollector를 추가하고 mixpanel 칸을 채우면 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    static final int DEFAULT_DAYS = 30;
    static final int MAX_DAYS = 400;
    static final int DEFAULT_API_LIMIT = 15;
    static final int MAX_API_LIMIT = 50;

    private final ServerStatsCollector serverStatsCollector;

    public AdminStatsResponse getStats(LocalDate from, LocalDate to, Integer apiLimit) {
        LocalDate today = LocalDate.now();
        StatsQuery query = toQuery(from, to, apiLimit, today);

        return new AdminStatsResponse(
                query.from(),
                query.to(),
                OffsetDateTime.now(),
                collectServer(query, today),
                SourceResult.notConfigured("Mixpanel 연동 전입니다."));
    }

    private SourceResult<ServerStats> collectServer(StatsQuery query, LocalDate today) {
        try {
            return SourceResult.ok(serverStatsCollector.collect(query, today));
        } catch (RuntimeException e) {
            log.error("관리자 통계 서버 집계 실패: {} ~ {}", query.from(), query.to(), e);
            return SourceResult.failed("서버 통계를 불러오지 못했습니다.");
        }
    }

    static StatsQuery toQuery(LocalDate from, LocalDate to, Integer apiLimit, LocalDate today) {
        LocalDate end = (to == null || to.isAfter(today)) ? today : to;
        LocalDate start = from == null ? end.minusDays(DEFAULT_DAYS - 1) : from;
        if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS) {
            throw new MyException(MyErrorCode.INVALID_STATS_RANGE);
        }
        int limit = apiLimit == null ? DEFAULT_API_LIMIT : Math.max(1, Math.min(apiLimit, MAX_API_LIMIT));
        return new StatsQuery(start, end, limit);
    }
}
