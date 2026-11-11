package kr.inuappcenterportal.inuportal.domain.stats;

import jakarta.persistence.EntityManager;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.stats.dto.ServerStats;
import kr.inuappcenterportal.inuportal.domain.stats.dto.ServerStats.LogStatus;
import kr.inuappcenterportal.inuportal.domain.stats.dto.StatsQuery;
import kr.inuappcenterportal.inuportal.domain.stats.service.ServerStatsCollector;
import kr.inuappcenterportal.inuportal.global.logging.domain.*;
import kr.inuappcenterportal.inuportal.global.logging.dto.res.LoggingApiResponse;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
    @DisplayName("일별 접속자: 존재하는 회원 ID만 회원, IP만 비회원으로 세고 '-1'·탈퇴 회원 ID는 어느 쪽에도 세지 않는다")
    void dailyVisits() {
        Member a = member("20260001", D1);
        Member b = member("20260002", D2);
        memberLog(D1, id(a), "1.2.3.4", "5.6.7.8", "-1", "99999");
        memberLog(D3, id(a), id(b), "2001:db8::1");

        ServerStats stats = collector.collect(new StatsQuery(D1, D3, 10), D3.plusDays(10));

        assertThat(stats.daily()).extracting(ServerStats.DailyPoint::date).containsExactly(D1, D2, D3);
        ServerStats.DailyPoint p1 = stats.daily().get(0);
        assertThat(p1.loginUsers()).isEqualTo(1);
        assertThat(p1.guestUsers()).isEqualTo(2);
        assertThat(p1.signups()).isEqualTo(1);
        assertThat(p1.logStatus()).isEqualTo(LogStatus.SUMMARY);
        assertThat(stats.daily().get(1).logStatus()).isEqualTo(LogStatus.MISSING);
        assertThat(stats.daily().get(1).signups()).isEqualTo(1);
        assertThat(stats.daily().get(2).loginUsers()).isEqualTo(2);
        assertThat(stats.daily().get(2).guestUsers()).isEqualTo(1);
        verifyNoInteractions(loggingService);
    }

    @Test
    @DisplayName("같은 날짜의 요약이 두 번 저장돼도 한 번만 센다")
    void duplicateSummaries() {
        Member a = member("20260001", D1);
        memberLog(D1, id(a), "1.2.3.4");
        memberLog(D1, id(a), "1.2.3.4");
        apiLog(D1, "GET", "/api/buses", 10L);
        summaryApiLogItemRepository.save(SummaryApiLogItem.of(
                summaryApiLogRepository.save(SummaryApiLog.from(D1)).getId(), 10L, "GET", "/api/buses"));

        ServerStats stats = collector.collect(new StatsQuery(D1, D1, 10), D3);

        assertThat(stats.daily().get(0).loginUsers()).isEqualTo(1);
        assertThat(stats.daily().get(0).guestUsers()).isEqualTo(1);
        assertThat(stats.apiTrends().get(0).total()).isEqualTo(10);
    }

    @Test
    @DisplayName("요약은 to 기준 최근 7·30일 접속 회원과 기간 내 가입자를 세고, 일부만 포함된 달은 partial로 표시한다")
    void summaryAndCohorts() {
        Member oldMember = member("20250001", LocalDate.of(2026, 9, 10));
        Member retained = member("20260001", D1);
        member("20260002", D2); // 가입 후 접속 없음
        memberLog(LocalDate.of(2026, 9, 20), id(oldMember), "777");
        memberLog(D3, id(retained));

        ServerStats stats = collector.collect(new StatsQuery(D1, D3, 10), D3.plusDays(10));

        assertThat(stats.summary().totalMembers()).isEqualTo(3);
        assertThat(stats.summary().activeMembers7d()).isEqualTo(1);
        assertThat(stats.summary().activeMembers30d()).isEqualTo(2);
        assertThat(stats.summary().signups()).isEqualTo(2);
        assertThat(stats.signupCohorts()).containsExactly(new ServerStats.SignupCohort("2026-10", 2, 1, true));

        ServerStats wholeMonths = collector.collect(
                new StatsQuery(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31), 10), LocalDate.of(2026, 12, 1));
        assertThat(wholeMonths.signupCohorts()).extracting(ServerStats.SignupCohort::partial).containsOnly(false);
    }

    @Test
    @DisplayName("요약 전인 오늘과 어제는 원본 로그를 날짜당 한 번만 읽어 일별·활성 회원·API 추이에 반영한다")
    void readsRawLogsBeforeSummary() {
        LocalDate today = D3;
        Member a = member("20260001", D1);
        Member b = member("20260002", D1);
        memberLog(D1, id(a));
        apiLog(D1, "GET", "/api/buses", 100L);
        given(loggingService.getRawMemberIds(D2)).willReturn(List.of(id(b), "8.8.8.8"));
        given(loggingService.getRawMemberIds(today)).willReturn(List.of(id(a), "9.9.9.9", "-1"));
        given(loggingService.getRawApiLogs(D2)).willReturn(List.of(LoggingApiResponse.of("GET", "/api/buses", 4L)));
        given(loggingService.getRawApiLogs(today)).willReturn(List.of(
                LoggingApiResponse.of("GET", "/api/buses", 7L),
                LoggingApiResponse.of("GET", "/api/agent/chat", 3L),
                LoggingApiResponse.of("CUSTOM", "/api/members/no-dup", 50L)));

        ServerStats stats = collector.collect(new StatsQuery(D1, today, 10), today);

        assertThat(stats.daily()).extracting(ServerStats.DailyPoint::logStatus)
                .containsExactly(LogStatus.SUMMARY, LogStatus.RAW, LogStatus.RAW);
        assertThat(stats.daily().get(1).loginUsers()).isEqualTo(1);
        assertThat(stats.daily().get(2).loginUsers()).isEqualTo(1);
        assertThat(stats.daily().get(2).guestUsers()).isEqualTo(1);
        assertThat(stats.summary().activeMembers30d()).isEqualTo(2);
        verify(loggingService, times(1)).getRawMemberIds(today);
        verify(loggingService, times(1)).getRawMemberIds(D2);

        assertThat(stats.apiTrends()).extracting(ServerStats.ApiTrend::uri)
                .containsExactly("/api/buses", "/api/agent/chat");
        ServerStats.ApiTrend bus = stats.apiTrends().get(0);
        assertThat(bus.daily()).containsExactly(100L, 4L, 7L);
        assertThat(bus.firstSeen()).isEqualTo(D1);
        assertThat(stats.apiTrends().get(1).firstSeen()).isEqualTo(today);
    }

    @Test
    @DisplayName("기간이 어제 이전에 끝나면 원본 로그를 읽지 않는다")
    void pastRangeSkipsRawLogs() {
        collector.collect(new StatsQuery(D1, D3, 10), D3.plusDays(2));
        verifyNoInteractions(loggingService);
    }

    @Test
    @DisplayName("API 추이는 호출 수 순(동점이면 URI 순)으로 apiLimit개만 주고, 첫 등장일은 기간 밖 기록까지 본다")
    void apiTrends() {
        apiLog(LocalDate.of(2026, 5, 11), "GET", "/api/chat-rooms", 1L);
        apiLog(D1, "GET", "/api/chat-rooms", 20L);
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
                    assertThat(t.total()).isEqualTo(20);
                });
    }

    private static String id(Member m) {
        return m.getId().toString();
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
