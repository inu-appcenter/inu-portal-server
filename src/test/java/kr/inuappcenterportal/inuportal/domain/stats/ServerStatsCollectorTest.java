package kr.inuappcenterportal.inuportal.domain.stats;

import jakarta.persistence.EntityManager;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.stats.dto.ServerStats;
import kr.inuappcenterportal.inuportal.domain.stats.dto.StatsQuery;
import kr.inuappcenterportal.inuportal.domain.stats.service.ServerStatsCollector;
import kr.inuappcenterportal.inuportal.global.logging.domain.*;
import kr.inuappcenterportal.inuportal.global.logging.dto.res.LoggingApiResponse;
import kr.inuappcenterportal.inuportal.global.logging.dto.res.LoggingMemberResponse;
import kr.inuappcenterportal.inuportal.global.logging.repository.*;
import kr.inuappcenterportal.inuportal.global.logging.service.LoggingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(ServerStatsCollector.class)
class ServerStatsCollectorTest {

    private static final LocalDate D1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate D2 = LocalDate.of(2026, 10, 2);
    private static final LocalDate D3 = LocalDate.of(2026, 10, 3);

    @Autowired ServerStatsCollector collector;
    @Autowired MemberRepository memberRepository;
    @Autowired SummaryMemberLogRepository summaryMemberLogRepository;
    @Autowired SummaryMemberLogItemRepository summaryMemberLogItemRepository;
    @Autowired SummaryApiLogRepository summaryApiLogRepository;
    @Autowired SummaryApiLogItemRepository summaryApiLogItemRepository;
    @Autowired EntityManager em;

    @MockBean LoggingService loggingService;

    @Test
    @DisplayName("요약 로그로 일별 접속자를 로그인/비로그인으로 나누고, 로그가 없는 날은 hasLog=false로 채운다")
    void dailyVisits() {
        Member a = member("20260001", D1);
        Member b = member("20260002", D2);
        memberLog(D1, a.getId().toString(), "1.2.3.4", "5.6.7.8");
        memberLog(D3, a.getId().toString(), b.getId().toString(), "2001:db8::1");

        ServerStats stats = collector.collect(new StatsQuery(D1, D3, 10), D3.plusDays(10));

        assertThat(stats.daily()).extracting(ServerStats.DailyPoint::date).containsExactly(D1, D2, D3);
        ServerStats.DailyPoint p1 = stats.daily().get(0);
        assertThat(p1.loginUsers()).isEqualTo(1);
        assertThat(p1.guestUsers()).isEqualTo(2);
        assertThat(p1.signups()).isEqualTo(1);
        assertThat(stats.daily().get(1).hasLog()).isFalse();
        assertThat(stats.daily().get(1).signups()).isEqualTo(1);
        assertThat(stats.daily().get(2).loginUsers()).isEqualTo(2);
        assertThat(stats.daily().get(2).guestUsers()).isEqualTo(1);
        verify(loggingService, never()).getMemberLogsByDate(any());
    }

    @Test
    @DisplayName("요약은 to 기준 최근 7·30일 접속 회원과 기간 내 가입자를 센다")
    void summaryAndCohorts() {
        Member oldMember = member("20250001", LocalDate.of(2026, 9, 10));
        Member retained = member("20260001", D1);
        member("20260002", D2); // 가입 후 접속 없음
        memberLog(LocalDate.of(2026, 9, 20), oldMember.getId().toString());
        memberLog(D3, retained.getId().toString());

        ServerStats stats = collector.collect(new StatsQuery(D1, D3, 10), D3.plusDays(10));

        assertThat(stats.summary().totalMembers()).isEqualTo(3);
        assertThat(stats.summary().activeMembers7d()).isEqualTo(1);
        assertThat(stats.summary().activeMembers30d()).isEqualTo(2);
        assertThat(stats.summary().signups()).isEqualTo(2);
        assertThat(stats.signupCohorts()).containsExactly(new ServerStats.SignupCohort("2026-10", 2, 1));
    }

    @Test
    @DisplayName("오늘은 요약 전이라 원본 로그를 한 번만 읽어 일별·활성 회원·API 추이에 반영한다")
    void includesToday() {
        LocalDate today = D2;
        Member a = member("20260001", D1);
        memberLog(D1, a.getId().toString());
        apiLog(D1, "GET", "/api/buses", 100L);
        given(loggingService.getMemberLogsByDate(today))
                .willReturn(LoggingMemberResponse.of(2, List.of("999", "9.9.9.9")));
        given(loggingService.getAPILogsByDate(today))
                .willReturn(List.of(LoggingApiResponse.of("GET", "/api/buses", 7L),
                        LoggingApiResponse.of("GET", "/api/agent/chat", 3L)));

        ServerStats stats = collector.collect(new StatsQuery(D1, today, 10), today);

        ServerStats.DailyPoint todayPoint = stats.daily().get(1);
        assertThat(todayPoint.loginUsers()).isEqualTo(1);
        assertThat(todayPoint.guestUsers()).isEqualTo(1);
        assertThat(todayPoint.hasLog()).isTrue();
        assertThat(stats.summary().activeMembers30d()).isEqualTo(2);
        verify(loggingService).getMemberLogsByDate(today);

        assertThat(stats.apiTrends()).hasSize(2);
        ServerStats.ApiTrend bus = stats.apiTrends().get(0);
        assertThat(bus.uri()).isEqualTo("/api/buses");
        assertThat(bus.daily()).containsExactly(100L, 7L);
        assertThat(bus.total()).isEqualTo(107);
        assertThat(bus.firstSeen()).isEqualTo(D1);
        assertThat(stats.apiTrends().get(1).firstSeen()).isEqualTo(today);
    }

    @Test
    @DisplayName("API 추이는 호출 수 순으로 apiLimit개만 주고, 첫 등장일은 기간 밖 기록까지 본다")
    void apiTrends() {
        apiLog(LocalDate.of(2026, 5, 11), "GET", "/api/chat-rooms", 1L);
        apiLog(D1, "GET", "/api/chat-rooms", 5L);
        apiLog(D1, "GET", "/api/buses", 50L);
        apiLog(D3, "GET", "/api/cafeterias", 20L);

        ServerStats stats = collector.collect(new StatsQuery(D1, D3, 2), D3.plusDays(10));

        assertThat(stats.apiTrends()).extracting(ServerStats.ApiTrend::uri)
                .containsExactly("/api/buses", "/api/cafeterias");
        assertThat(stats.apiTrends().get(1).daily()).containsExactly(0L, 0L, 20L);

        ServerStats all = collector.collect(new StatsQuery(D1, D3, 10), D3.plusDays(10));
        assertThat(all.apiTrends()).filteredOn(t -> t.uri().equals("/api/chat-rooms"))
                .singleElement()
                .satisfies(t -> {
                    assertThat(t.firstSeen()).isEqualTo(LocalDate.of(2026, 5, 11));
                    assertThat(t.total()).isEqualTo(5);
                });
    }

    private Member member(String studentId, LocalDate joinedOn) {
        Member m = memberRepository.save(Member.builder().studentId(studentId).roles(List.of("ROLE_USER")).build());
        em.flush();
        em.createNativeQuery("UPDATE member SET joined_at = :t WHERE id = :id")
                .setParameter("t", LocalDateTime.of(joinedOn, java.time.LocalTime.NOON))
                .setParameter("id", m.getId())
                .executeUpdate();
        return m;
    }

    private void memberLog(LocalDate date, String... memberIds) {
        SummaryMemberLog log = summaryMemberLogRepository.save(SummaryMemberLog.of(memberIds.length, date));
        for (String id : memberIds) {
            summaryMemberLogItemRepository.save(SummaryMemberLogItem.of(log.getId(), id));
        }
    }

    private void apiLog(LocalDate date, String method, String uri, Long count) {
        Long logId = summaryApiLogRepository.findIdByDate(date)
                .orElseGet(() -> summaryApiLogRepository.save(SummaryApiLog.from(date)).getId());
        summaryApiLogItemRepository.save(SummaryApiLogItem.of(logId, count, method, uri));
    }
}
