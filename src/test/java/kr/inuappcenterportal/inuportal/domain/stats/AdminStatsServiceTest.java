package kr.inuappcenterportal.inuportal.domain.stats.service;

import kr.inuappcenterportal.inuportal.domain.stats.dto.AdminStatsResponse;
import kr.inuappcenterportal.inuportal.domain.stats.dto.SourceResult;
import kr.inuappcenterportal.inuportal.domain.stats.dto.StatsQuery;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class AdminStatsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);

    @Test
    @DisplayName("기간을 생략하면 오늘까지 최근 30일, 미래 종료일은 오늘로 맞춘다")
    void defaults() {
        StatsQuery q = AdminStatsService.toQuery(null, null, null, TODAY);
        assertThat(q.from()).isEqualTo(TODAY.minusDays(29));
        assertThat(q.to()).isEqualTo(TODAY);
        assertThat(q.apiLimit()).isEqualTo(15);

        assertThat(AdminStatsService.toQuery(TODAY.minusDays(3), TODAY.plusDays(5), 999, TODAY))
                .isEqualTo(new StatsQuery(TODAY.minusDays(3), TODAY, 50));
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦거나 400일을 넘으면 400 에러")
    void invalidRange() {
        assertThatThrownBy(() -> AdminStatsService.toQuery(TODAY, TODAY.minusDays(1), null, TODAY))
                .isInstanceOfSatisfying(MyException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(MyErrorCode.INVALID_STATS_RANGE));
        assertThatThrownBy(() -> AdminStatsService.toQuery(TODAY.minusDays(400), TODAY, null, TODAY))
                .isInstanceOf(MyException.class);
        assertThat(AdminStatsService.toQuery(TODAY.minusDays(399), TODAY, null, TODAY).from())
                .isEqualTo(TODAY.minusDays(399));
    }

    @Test
    @DisplayName("서버 집계가 실패해도 응답은 나가고, 내부 예외 문구는 노출하지 않는다")
    void serverFailureIsIsolated() {
        ServerStatsCollector collector = mock(ServerStatsCollector.class);
        given(collector.collect(any(), any())).willThrow(new IllegalStateException("SQL grammar ..."));

        AdminStatsResponse res = new AdminStatsService(collector).getStats(null, null, null);

        assertThat(res.server().status()).isEqualTo(SourceResult.Status.FAILED);
        assertThat(res.server().message()).doesNotContain("SQL");
        assertThat(res.mixpanel().status()).isEqualTo(SourceResult.Status.NOT_CONFIGURED);
    }
}
