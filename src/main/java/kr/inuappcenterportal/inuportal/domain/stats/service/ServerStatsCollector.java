package kr.inuappcenterportal.inuportal.domain.stats.service;

import kr.inuappcenterportal.inuportal.domain.stats.dto.ServerStats;
import kr.inuappcenterportal.inuportal.domain.stats.dto.ServerStats.LogStatus;
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
 * 지난 날짜는 자정에 만드는 요약(summary_*)을 읽는다. 요약 전인 오늘과, 자정 직후 요약 작업이 끝나기 전의 어제는 원본 로그를 읽는다.
 */
@Component
@RequiredArgsConstructor
public class ServerStatsCollector {

    /** 접속 집계용으로 클라이언트가 보내는 신호라 API 추이에서 뺀다. */
    private static final String VISIT_PING_URI = "/api/members/no-dup";

    private final AdminStatsRepository statsRepository;
    private final LoggingService loggingService;

    @Transactional(readOnly = true)
    public ServerStats collect(StatsQuery query, LocalDate today) {
        LocalDate from = query.from();
        LocalDate to = query.to();
        List<LocalDate> days = from.datesUntil(to.plusDays(1)).toList();
        Set<String> existingMemberIds = statsRepository.findAllMemberIds().stream()
                .map(String::valueOf).collect(Collectors.toSet());

        // 요약 로그의 접속 ID. 최근 30일 활성 회원 계산을 위해 to-29일부터 읽는다
        LocalDate visitorsFrom = from.isBefore(to.minusDays(29)) ? from : to.minusDays(29);
        Set<LocalDate> summarized = new HashSet<>(statsRepository.findSummarizedDates(visitorsFrom, to));
        Map<LocalDate, List<String>> idsByDay = new HashMap<>();
        statsRepository.findDailyVisitors(visitorsFrom, to)
                .forEach(v -> idsByDay.computeIfAbsent(v.date(), k -> new ArrayList<>()).add(v.memberId()));

        // 요약이 없는 오늘·어제는 원본 로그를 날짜당 한 번씩만 읽는다
        Set<LocalDate> rawDays = new HashSet<>();
        for (LocalDate d : List.of(today.minusDays(1), today)) {
            if (!d.isBefore(visitorsFrom) && !d.isAfter(to) && !summarized.contains(d)) {
                idsByDay.put(d, loggingService.getRawMemberIds(d));
                rawDays.add(d);
            }
        }

        List<StatsRows.MemberJoin> joins = statsRepository.findMemberJoins(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        Map<LocalDate, Long> signupsByDay = joins.stream()
                .collect(Collectors.groupingBy(j -> j.joinedAt().toLocalDate(), Collectors.counting()));

        List<ServerStats.DailyPoint> daily = days.stream().map(d -> {
            long signups = signupsByDay.getOrDefault(d, 0L);
            List<String> ids = idsByDay.getOrDefault(d, List.of());
            boolean raw = rawDays.contains(d);
            if (summarized.contains(d) || (raw && !ids.isEmpty())) {
                return new ServerStats.DailyPoint(d,
                        ids.stream().filter(existingMemberIds::contains).distinct().count(),
                        ids.stream().filter(ServerStatsCollector::isGuestId).distinct().count(),
                        signups, raw ? LogStatus.RAW : LogStatus.SUMMARY);
            }
            return new ServerStats.DailyPoint(d, 0, 0, signups, LogStatus.MISSING);
        }).toList();

        // to 기준 최근 7일·30일 접속 회원
        Set<String> active30 = activeMemberIds(to.minusDays(29), to, idsByDay, existingMemberIds);
        Set<String> active7 = activeMemberIds(to.minusDays(6), to, idsByDay, existingMemberIds);

        ServerStats.Summary summary = new ServerStats.Summary(
                existingMemberIds.size(), active7.size(), active30.size(), joins.size());

        return new ServerStats(summary, daily, signupCohorts(joins, active30, from, to),
                apiTrends(query, days, summarized, today));
    }

    private Set<String> activeMemberIds(LocalDate from, LocalDate to, Map<LocalDate, List<String>> idsByDay,
                                        Set<String> existingMemberIds) {
        Set<String> ids = new HashSet<>();
        idsByDay.forEach((d, list) -> {
            if (!d.isBefore(from) && !d.isAfter(to)) {
                list.stream().filter(existingMemberIds::contains).forEach(ids::add);
            }
        });
        return ids;
    }

    private List<ServerStats.SignupCohort> signupCohorts(List<StatsRows.MemberJoin> joins, Set<String> active30,
                                                         LocalDate from, LocalDate to) {
        Map<YearMonth, List<StatsRows.MemberJoin>> byMonth = joins.stream()
                .collect(Collectors.groupingBy(j -> YearMonth.from(j.joinedAt()), TreeMap::new, Collectors.toList()));
        return byMonth.entrySet().stream().map(e -> {
            YearMonth month = e.getKey();
            boolean partial = month.atDay(1).isBefore(from) || month.atEndOfMonth().isAfter(to);
            return new ServerStats.SignupCohort(
                    month.toString(),
                    e.getValue().size(),
                    e.getValue().stream().filter(j -> active30.contains(j.memberId().toString())).count(),
                    partial);
        }).toList();
    }

    private List<ServerStats.ApiTrend> apiTrends(StatsQuery query, List<LocalDate> days, Set<LocalDate> summarized,
                                                 LocalDate today) {
        LocalDate from = query.from();
        List<StatsRows.ApiDaily> rows = new ArrayList<>(statsRepository.findApiDaily(from, query.to()));
        for (LocalDate d : List.of(today.minusDays(1), today)) {
            if (!d.isBefore(from) && !d.isAfter(query.to()) && !summarized.contains(d)) {
                for (LoggingApiResponse r : loggingService.getRawApiLogs(d)) {
                    rows.add(new StatsRows.ApiDaily(d, r.method(), r.uri(), r.apiCount()));
                }
            }
        }

        Map<String, LocalDate> firstSeen = new HashMap<>();
        statsRepository.findApiFirstSeen().forEach(f -> firstSeen.put(key(f.method(), f.uri()), f.firstSeen()));

        Map<String, long[]> series = new HashMap<>();
        Map<String, String[]> names = new HashMap<>();
        for (StatsRows.ApiDaily r : rows) {
            if (VISIT_PING_URI.equals(r.uri())) continue;
            String k = key(r.method(), r.uri());
            int idx = (int) ChronoUnit.DAYS.between(from, r.date());
            series.computeIfAbsent(k, x -> new long[days.size()])[idx] += r.count();
            names.putIfAbsent(k, new String[]{r.method(), r.uri()});
            // 아직 요약에 없는 API는 원본 로그에 처음 나온 날을 첫 등장일로 본다
            firstSeen.merge(k, r.date(), (a, b) -> a.isBefore(b) ? a : b);
        }

        return series.entrySet().stream()
                .map(e -> {
                    String[] n = names.get(e.getKey());
                    long[] counts = e.getValue();
                    return new ServerStats.ApiTrend(n[0], n[1], firstSeen.get(e.getKey()),
                            Arrays.stream(counts).sum(), Arrays.stream(counts).boxed().toList());
                })
                .sorted(Comparator.comparingLong(ServerStats.ApiTrend::total).reversed()
                        .thenComparing(ServerStats.ApiTrend::uri)
                        .thenComparing(ServerStats.ApiTrend::method))
                .limit(query.apiLimit())
                .toList();
    }

    private static String key(String method, String uri) {
        return method + " " + uri;
    }

    /** 비로그인 방문자 ID는 IP다. */
    static boolean isGuestId(String id) {
        return id != null && (id.contains(".") || id.contains(":"));
    }
}
