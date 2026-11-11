package kr.inuappcenterportal.inuportal.domain.stats.service;

import kr.inuappcenterportal.inuportal.domain.stats.dto.ServerStats;
import kr.inuappcenterportal.inuportal.domain.stats.dto.StatsQuery;
import kr.inuappcenterportal.inuportal.domain.stats.repository.AdminStatsRepository;
import kr.inuappcenterportal.inuportal.domain.stats.repository.StatsRows;
import kr.inuappcenterportal.inuportal.global.logging.dto.res.LoggingApiResponse;
import kr.inuappcenterportal.inuportal.global.logging.service.LoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 서버 로그 요약과 회원 테이블로 통계를 만든다.
 * 지난 날짜는 자정에 만들어지는 요약(summary_*)을 읽고, 오늘은 아직 요약 전이라 원본 로그를 읽는다.
 */
@Component
@RequiredArgsConstructor
public class ServerStatsCollector {

    private final AdminStatsRepository statsRepository;
    private final LoggingService loggingService;

    @Transactional(readOnly = true)
    public ServerStats collect(StatsQuery query, LocalDate today) {
        LocalDate from = query.from();
        LocalDate to = query.to();
        List<LocalDate> days = from.datesUntil(to.plusDays(1)).toList();
        boolean includesToday = !to.isBefore(today);

        // 일별 접속자
        Map<LocalDate, StatsRows.DailyVisit> visits = new HashMap<>();
        statsRepository.findDailyVisits(from, to).forEach(v -> visits.put(v.date(), v));
        Set<LocalDate> loggedDates = new HashSet<>(statsRepository.findLoggedDates(from, to));
        // 오늘은 요약 전이라 원본 로그를 한 번만 읽어 재사용한다
        List<String> todayIds = includesToday ? loggingService.getMemberLogsByDate(today).memberIds() : List.of();
        if (includesToday) {
            long login = todayIds.stream().filter(ServerStatsCollector::isLoginMemberId).count();
            visits.put(today, new StatsRows.DailyVisit(today, login, todayIds.size() - login));
            loggedDates.add(today);
        }

        // 기간 + 코호트 판정용 가입자
        List<StatsRows.MemberJoin> joins = statsRepository.findMemberJoins(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        Map<LocalDate, Long> signupsByDay = joins.stream()
                .collect(Collectors.groupingBy(j -> j.joinedAt().toLocalDate(), Collectors.counting()));

        List<ServerStats.DailyPoint> daily = days.stream().map(d -> {
            StatsRows.DailyVisit v = visits.get(d);
            return new ServerStats.DailyPoint(
                    d,
                    v == null ? 0 : v.loginUsers(),
                    v == null ? 0 : v.guestUsers(),
                    signupsByDay.getOrDefault(d, 0L),
                    loggedDates.contains(d));
        }).toList();

        // to 기준 최근 7일·30일 접속 회원
        Set<String> active30 = activeMemberIds(to.minusDays(29), to, todayIds);
        Set<String> active7 = activeMemberIds(to.minusDays(6), to, todayIds);

        ServerStats.Summary summary = new ServerStats.Summary(
                statsRepository.countMembers(), active7.size(), active30.size(), joins.size());

        return new ServerStats(summary, daily, signupCohorts(joins, active30), apiTrends(query, days, today));
    }

    private Set<String> activeMemberIds(LocalDate from, LocalDate to, List<String> todayIds) {
        Set<String> ids = new HashSet<>(statsRepository.findDistinctLoginMemberIds(from, to));
        todayIds.stream().filter(ServerStatsCollector::isLoginMemberId).forEach(ids::add);
        return ids;
    }

    private List<ServerStats.SignupCohort> signupCohorts(List<StatsRows.MemberJoin> joins, Set<String> active30) {
        Map<YearMonth, List<StatsRows.MemberJoin>> byMonth = joins.stream()
                .collect(Collectors.groupingBy(j -> YearMonth.from(j.joinedAt()), TreeMap::new, Collectors.toList()));
        return byMonth.entrySet().stream().map(e -> new ServerStats.SignupCohort(
                e.getKey().toString(),
                e.getValue().size(),
                e.getValue().stream().filter(j -> active30.contains(j.memberId().toString())).count()
        )).toList();
    }

    private List<ServerStats.ApiTrend> apiTrends(StatsQuery query, List<LocalDate> days, LocalDate today) {
        LocalDate from = query.from();
        List<StatsRows.ApiDaily> rows = new ArrayList<>(statsRepository.findApiDaily(from, query.to()));
        if (!query.to().isBefore(today)) {
            for (LoggingApiResponse r : loggingService.getAPILogsByDate(today)) {
                rows.add(new StatsRows.ApiDaily(today, r.method(), r.uri(), r.apiCount()));
            }
        }

        Map<String, LocalDate> firstSeen = new HashMap<>();
        statsRepository.findApiFirstSeen().forEach(f -> firstSeen.put(key(f.method(), f.uri()), f.firstSeen()));

        Map<String, long[]> series = new HashMap<>();
        Map<String, String[]> names = new HashMap<>();
        for (StatsRows.ApiDaily r : rows) {
            String k = key(r.method(), r.uri());
            int idx = (int) ChronoUnit.DAYS.between(from, r.date());
            series.computeIfAbsent(k, x -> new long[days.size()])[idx] += r.count();
            names.putIfAbsent(k, new String[]{r.method(), r.uri()});
            // 오늘 처음 나타난 API는 요약에 없으므로 오늘을 첫 등장일로 본다
            firstSeen.merge(k, r.date(), (a, b) -> a.isBefore(b) ? a : b);
        }

        return series.entrySet().stream()
                .map(e -> {
                    String[] n = names.get(e.getKey());
                    long[] counts = e.getValue();
                    return new ServerStats.ApiTrend(n[0], n[1], firstSeen.get(e.getKey()),
                            Arrays.stream(counts).sum(), Arrays.stream(counts).boxed().toList());
                })
                .sorted(Comparator.comparingLong(ServerStats.ApiTrend::total).reversed())
                .limit(query.apiLimit())
                .toList();
    }

    private static String key(String method, String uri) {
        return method + " " + uri;
    }

    static boolean isLoginMemberId(String memberId) {
        return memberId != null && !memberId.isEmpty() && memberId.chars().allMatch(Character::isDigit);
    }
}
