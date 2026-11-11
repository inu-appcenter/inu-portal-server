package kr.inuappcenterportal.inuportal.domain.stats.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 통계 쿼리 결과 행.
 */
public final class StatsRows {

    private StatsRows() {
    }

    public record DailyVisit(LocalDate date, Long loginUsers, Long guestUsers) {
    }

    public record MemberJoin(Long memberId, LocalDateTime joinedAt) {
    }

    public record ApiDaily(LocalDate date, String method, String uri, Long count) {
    }

    public record ApiFirstSeen(String method, String uri, LocalDate firstSeen) {
    }
}
