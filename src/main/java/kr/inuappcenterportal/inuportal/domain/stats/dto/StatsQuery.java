package kr.inuappcenterportal.inuportal.domain.stats.dto;

import java.time.LocalDate;

/**
 * 통계 조회 조건. 모든 데이터 출처(서버, Mixpanel)가 같은 조건을 받는다.
 */
public record StatsQuery(LocalDate from, LocalDate to, int apiLimit) {
}
