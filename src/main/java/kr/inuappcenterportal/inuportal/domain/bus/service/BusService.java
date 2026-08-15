package kr.inuappcenterportal.inuportal.domain.bus.service;

import kr.inuappcenterportal.inuportal.domain.bus.dto.*;
import kr.inuappcenterportal.inuportal.domain.bus.entity.BusArrivalHistory;
import kr.inuappcenterportal.inuportal.domain.bus.entity.BusRouteSection;
import kr.inuappcenterportal.inuportal.domain.bus.entity.BusRouteStop;
import kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetStop;
import kr.inuappcenterportal.inuportal.domain.bus.repository.BusArrivalHistoryRepository;
import kr.inuappcenterportal.inuportal.domain.bus.repository.BusRouteSectionRepository;
import kr.inuappcenterportal.inuportal.domain.bus.repository.BusTargetStopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BusService {

    private final BusApiService busApiService;
    private final BusArrivalHistoryRepository busArrivalHistoryRepository;
    private final BusRouteSectionRepository busRouteSectionRepository;
    private final BusTargetStopRepository busTargetStopRepository;
    private final kr.inuappcenterportal.inuportal.domain.bus.repository.BusTargetRuleRepository busTargetRuleRepository;
    private final kr.inuappcenterportal.inuportal.domain.bus.repository.BusStopAliasRepository busStopAliasRepository;

    public List<BusStopSearchDto> searchBusStops(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        String trimmed = keyword.trim();
        List<BusStopSearchDto> results = new ArrayList<>();

        // 1. 공공데이터 API 검색 결과
        List<BusStopSearchDto> searched = busApiService.searchBusStops(trimmed);
        results.addAll(searched);

        // 2. 로컬 별칭 사전 및 기등록 정류소에서 단축번호/ID/명칭/별칭 매칭
        List<kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias> aliases = busStopAliasRepository.findAll();
        for (kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias a : aliases) {
            boolean matches = (a.getBstopId() != null && a.getBstopId().contains(trimmed)) ||
                    (a.getBstopName() != null && a.getBstopName().contains(trimmed)) ||
                    (a.getStopAlias() != null && a.getStopAlias().contains(trimmed));

            if (matches) {
                boolean alreadyExists = results.stream().anyMatch(r -> r.getBstopId().equals(a.getBstopId()));
                if (!alreadyExists) {
                    results.add(0, BusStopSearchDto.builder()
                            .bstopId(a.getBstopId())
                            .bstopName(a.getBstopName())
                            .stopAlias(a.getStopAlias())
                            .build());
                }
            }
        }

        // 별칭 매핑 보완 및 중복 제거
        return results.stream()
                .filter(distinctByKey(BusStopSearchDto::getBstopId))
                .map(s -> {
                    String alias = busStopAliasRepository.findByBstopId(s.getBstopId())
                            .map(kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias::getStopAlias)
                            .orElse(s.getStopAlias());
                    return BusStopSearchDto.builder()
                            .bstopId(s.getBstopId())
                            .bstopName(s.getBstopName())
                            .bstopNo(s.getBstopNo())
                            .adminNm(s.getAdminNm())
                            .latitude(s.getLatitude())
                            .longitude(s.getLongitude())
                            .stopAlias(alias)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private static <T> java.util.function.Predicate<T> distinctByKey(java.util.function.Function<? super T, ?> keyExtractor) {
        java.util.Set<Object> seen = java.util.concurrent.ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }


    @Transactional
    public List<BusStopAliasDto> getStopAliases() {
        List<kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias> list = busStopAliasRepository.findAll();
        if (list.isEmpty()) {
            // 기본 주요 정류장 별칭 및 안내 문구(stopNotice) 초기화
            List<kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias> defaults = List.of(
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias.builder()
                            .bstopId("164000395").bstopName("인천대입구역 2번출구").stopAlias("인입")
                            .stopNotice("※ 8시 ~ 10시에는 매우 혼잡해요. 계단에서 줄서기를 꼭 지켜주세요.")
                            .memo("인입런 메인 출발 정류소").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias.builder()
                            .bstopId("164000396").bstopName("인천대입구역 1번출구").stopAlias("인입")
                            .stopNotice("※ 에스컬레이터가 있어 이동하기 편리합니다.")
                            .memo("인입런 1번출구 정류소").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias.builder()
                            .bstopId("164000648").bstopName("인천대입구역.롯데몰").stopAlias("인입")
                            .stopNotice("※ 청라에서 오셨나요? 이 정류장에서 환승도 좋은 선택지에요.")
                            .memo("인입런 롯데몰 정류소").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias.builder()
                            .bstopId("164000403").bstopName("지식정보단지역 3번출구").stopAlias("지정단")
                            .stopNotice("※ 엘리베이터를 타면 정류장을 쉽게 찾을 수 있어요.")
                            .memo("지정단런 출발 정류소").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias.builder()
                            .bstopId("164000404").bstopName("지식정보단지역 2번출구").stopAlias("지정단")
                            .stopNotice("※ 6-1번 하교 정류소")
                            .memo("지정단런 하교 정류소").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias.builder()
                            .bstopId("164000385").bstopName("인천대 정문(길 건너)").stopAlias("정문")
                            .stopNotice("※ 인문대 학생들이 이용하기 좋아요.")
                            .memo("하교 정문 정류소").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias.builder()
                            .bstopId("164000386").bstopName("인천대 정문(앞)").stopAlias("정문")
                            .stopNotice("※ 정류장 위치를 꼭 확인해주세요!")
                            .memo("하교 정문앞 정류소").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias.builder()
                            .bstopId("164000377").bstopName("인천대 공과대학").stopAlias("공대")
                            .stopNotice("※ 이 곳은 출발지라 도착 정보가 표시되지 않습니다.\n자연대 정류장을 참고해주세요.")
                            .memo("하교 공과대 출발 정류소").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias.builder()
                            .bstopId("164000499").bstopName("인천대 공과대학").stopAlias("공대")
                            .stopNotice("※ 공과대학 종점 하차 정류소")
                            .memo("8번/46번 등교 공과대 종점").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias.builder()
                            .bstopId("164000376").bstopName("인천대 공과대학").stopAlias("공대")
                            .stopNotice("※ 6-1번 공과대학 종점")
                            .memo("6-1번 등교 공과대 종점").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias.builder()
                            .bstopId("164000378").bstopName("인천대 자연과학대학").stopAlias("자연대")
                            .stopNotice("※ 오후 4~6시에는 사람이 몰려 버스가 정차하지 않을 수 있어요. 공과대학 정류장 이용을 추천해요.")
                            .memo("하교 자연대 정류소").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias.builder()
                            .bstopId("164000375").bstopName("인천대 자연과학대학").stopAlias("자연대")
                            .stopNotice("※ 자연과학대학 하차 정류소")
                            .memo("등교 자연대 종점방면 정류소").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias.builder()
                            .bstopId("164000751").bstopName("인천대 송도캠퍼스(기숙사)").stopAlias("기숙사")
                            .stopNotice("※ 암벽장 앞, 기숙사 근처에 위치해 있어요.\n※ 버스가 오지 않을 때는 공과대 정류장을 이용해보세요!")
                            .memo("하교 기숙사 정류소").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias.builder()
                            .bstopId("164000763").bstopName("인천대 송도캠퍼스(기숙사)").stopAlias("기숙사")
                            .stopNotice("※ 순환41번 기숙사 종점 하차 정류소")
                            .memo("41번 등교 기숙사 종점").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias.builder()
                            .bstopId("164000752").bstopName("인천대북문").stopAlias("북문")
                            .stopNotice("※ 북문 정류소")
                            .memo("북문 하교 방면").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias.builder()
                            .bstopId("164000762").bstopName("인천대북문").stopAlias("북문")
                            .stopNotice("※ 북문 정류소")
                            .memo("41번 등교 북문 방면").build()
            );
            busStopAliasRepository.saveAll(defaults);
            list = busStopAliasRepository.findAll();
        }

        return list.stream()
                .map(a -> BusStopAliasDto.builder()
                        .id(a.getId())
                        .bstopId(a.getBstopId())
                        .bstopName(a.getBstopName())
                        .stopAlias(a.getStopAlias())
                        .stopNotice(a.getStopNotice())
                        .memo(a.getMemo())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public BusStopAliasDto saveStopAlias(BusStopAliasDto dto) {
        kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias alias = busStopAliasRepository.findByBstopId(dto.getBstopId())
                .map(existing -> {
                    existing.update(dto.getBstopName(), dto.getStopAlias(), dto.getStopNotice(), dto.getMemo());
                    return existing;
                })
                .orElseGet(() -> kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias.builder()
                        .bstopId(dto.getBstopId())
                        .bstopName(dto.getBstopName())
                        .stopAlias(dto.getStopAlias())
                        .stopNotice(dto.getStopNotice())
                        .memo(dto.getMemo())
                        .build());

        kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias saved = busStopAliasRepository.save(alias);

        // 수집 대상 정류소(BusTargetStop)에도 별칭 동기화
        busTargetStopRepository.findByBstopId(dto.getBstopId()).ifPresent(targetStop -> {
            targetStop.updateStopAlias(dto.getStopAlias());
        });


        return BusStopAliasDto.builder()
                .id(saved.getId())
                .bstopId(saved.getBstopId())
                .bstopName(saved.getBstopName())
                .stopAlias(saved.getStopAlias())
                .stopNotice(saved.getStopNotice())
                .memo(saved.getMemo())
                .build();
    }


    @Transactional
    public void deleteStopAlias(Long id) {
        busStopAliasRepository.deleteById(id);
    }



    public List<BusArrivalItemDto> getRealtimeArrivals(String bstopId) {
        List<BusArrivalItemDto> arrivals = busApiService.fetchBusArrivals(bstopId);

        if (arrivals.isEmpty()) {
            // 실시간 정보가 없는 경우 통계 기반 추정치 추가
            return calculateEstimatedArrivals(bstopId);
        }

        return arrivals;
    }

    private static final java.util.Map<String, String> ROUTE_NO_FALLBACK = java.util.Map.ofEntries(
            java.util.Map.entry("165000012", "8"),
            java.util.Map.entry("165000020", "16"),
            java.util.Map.entry("165000514", "41"),
            java.util.Map.entry("164000004", "46"),
            java.util.Map.entry("165000008", "6-1"),
            java.util.Map.entry("165000007", "6"),
            java.util.Map.entry("165000150", "1301"),
            java.util.Map.entry("161000007", "9"),
            java.util.Map.entry("161000027", "순환43"),
            java.util.Map.entry("161000038", "순환42"),
            java.util.Map.entry("165000201", "순환47"),
            java.util.Map.entry("213000019", "3002")
    );

    public BusHistoryResponseDto getHistory(String bstopId, String targetDateStr) {
        LocalDate targetDate = (targetDateStr != null && !targetDateStr.isBlank())
                ? LocalDate.parse(targetDateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                : LocalDate.now();

        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(LocalTime.MAX);

        List<BusArrivalHistory> historyList = busArrivalHistoryRepository
                .findByBstopIdAndCreateDateBetweenOrderByCreateDateAsc(bstopId, startOfDay, endOfDay);

        // 1. 등록된 노선 정보에서 routeId -> routeNo 맵 구성
        java.util.Map<String, String> sectionRouteMap = busRouteSectionRepository.findAll().stream()
                .filter(s -> s.getRouteId() != null && s.getRouteNo() != null)
                .collect(Collectors.toMap(
                        BusRouteSection::getRouteId,
                        BusRouteSection::getRouteNo,
                        (existing, replacement) -> existing
                ));

        // 2. 실제 해당 정류소에 접근/도착한 기록만 필터링 및 7분 이내 동일 차량 중복 제거
        List<BusHistoryResponseDto.HistoryRecord> records = new ArrayList<>();
        java.util.Map<String, LocalDateTime> lastArrivalSeen = new java.util.HashMap<>();

        for (BusArrivalHistory h : historyList) {
            Integer restStops = h.getRestStopCount();
            Integer estimateTime = h.getArrivalEstimateTime();

            // 실제 도착(0~1정류장 전 또는 90초 이내)한 기록만 인정
            boolean isArriving = (restStops != null && restStops <= 1)
                    || (estimateTime != null && estimateTime > 0 && estimateTime <= 90);

            if (!isArriving) {
                continue;
            }

            String vehicleKey = (h.getBusNumPlate() != null && !h.getBusNumPlate().isBlank())
                    ? h.getBusNumPlate()
                    : (h.getRouteId() + "_" + h.getCreateDate().getMinute());

            LocalDateTime lastTime = lastArrivalSeen.get(vehicleKey);
            if (lastTime != null && h.getCreateDate().isBefore(lastTime.plusMinutes(7))) {
                continue; // 7분 이내 동일 차량 중복 무시
            }
            lastArrivalSeen.put(vehicleKey, h.getCreateDate());

            String routeNo = sectionRouteMap.get(h.getRouteId());
            if (routeNo == null || routeNo.isBlank()) {
                routeNo = ROUTE_NO_FALLBACK.getOrDefault(h.getRouteId(), "");
            }

            records.add(BusHistoryResponseDto.HistoryRecord.builder()
                    .id(h.getId())
                    .routeId(h.getRouteId())
                    .routeNo(routeNo)
                    .busNumPlate(h.getBusNumPlate())
                    .restStopCount(h.getRestStopCount())
                    .arrivalEstimateTime(h.getArrivalEstimateTime())
                    .arrivalTime(h.getCreateDate())
                    .build());
        }

        // 동일 요일 최근 4주 평균 간격/소요시간 계산
        Integer averageInterval = calculateAverageIntervalForDayOfWeek(bstopId, targetDate.getDayOfWeek());

        return BusHistoryResponseDto.builder()
                .bstopId(bstopId)
                .targetDate(targetDate.toString())
                .dayOfWeek(targetDate.getDayOfWeek().name())
                .historyRecords(records)
                .averageIntervalSeconds(averageInterval)
                .build();
    }

    public List<BusRouteSectionResponseDto> getRouteSections(String category) {
        List<BusRouteSection> sections = (category != null && !category.isBlank())
                ? busRouteSectionRepository.findByCategory(category)
                : busRouteSectionRepository.findAll();

        return sections.stream()
                .map(section -> {
                    BusRouteSectionResponseDto dto = BusRouteSectionResponseDto.from(section);
                    String combinedNotice = dto.getBusNotice();

                    if (combinedNotice == null || combinedNotice.isBlank()) {
                        String dbFirstLastTime = calculateStopFirstLastTime(section.getStartBstopId(), section.getRouteId());
                        String allocGap = busApiService.fetchAllocGap(section.getRouteId());

                        if (dbFirstLastTime != null && allocGap != null) {
                            combinedNotice = dbFirstLastTime + "\n" + allocGap;
                        } else if (dbFirstLastTime != null) {
                            combinedNotice = dbFirstLastTime;
                        } else if (allocGap != null) {
                            combinedNotice = allocGap;
                        }
                    }

                    String alias = busStopAliasRepository.findByBstopId(section.getStartBstopId())
                            .map(kr.inuappcenterportal.inuportal.domain.bus.entity.BusStopAlias::getStopAlias)
                            .orElseGet(() -> busTargetStopRepository.findByBstopId(section.getStartBstopId())
                                    .map(BusTargetStop::getStopAlias)
                                    .orElse(null));


                    return BusRouteSectionResponseDto.builder()
                            .id(dto.getId())
                            .sectionName(dto.getSectionName())
                            .category(dto.getCategory())
                            .tabName(dto.getTabName())
                            .routeNo(dto.getRouteNo())
                            .routeId(dto.getRouteId())
                            .startBstopId(dto.getStartBstopId())
                            .startBstopName(dto.getStartBstopName())
                            .startBstopAlias(alias)
                            .endBstopId(dto.getEndBstopId())
                            .endBstopName(dto.getEndBstopName())
                            .busNotice(combinedNotice)
                            .routeNotice(dto.getRouteNotice())
                            .stops(dto.getStops())
                            .build();
                })
                .collect(Collectors.toList());
    }



    public String calculateStopFirstLastTime(String bstopId, String routeId) {
        if (bstopId == null || routeId == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        List<LocalTime> arrivalTimes = new ArrayList<>();

        for (int i = 0; i < 28; i++) {
            LocalDate pastDate = now.toLocalDate().minusDays(i);
            if (pastDate.getDayOfWeek() == now.getDayOfWeek()) {
                LocalDateTime startOfDay = pastDate.atStartOfDay();
                LocalDateTime endOfDay = pastDate.atTime(LocalTime.MAX);

                List<BusArrivalHistory> logs = busArrivalHistoryRepository
                        .findByBstopIdAndRouteIdAndCreateDateBetweenOrderByCreateDateAsc(bstopId, routeId, startOfDay, endOfDay);

                for (BusArrivalHistory logItem : logs) {
                    if (logItem.getArrivalEstimateTime() != null && logItem.getArrivalEstimateTime() <= 180) {
                        arrivalTimes.add(logItem.getCreateDate().toLocalTime());
                    }
                }
            }
        }

        if (arrivalTimes.isEmpty()) {
            return null;
        }

        LocalTime earliest = arrivalTimes.stream().min(LocalTime::compareTo).orElse(null);
        LocalTime latest = arrivalTimes.stream().max(LocalTime::compareTo).orElse(null);

        if (earliest == null || latest == null) {
            return null;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        return String.format("해당 정류소 실측 첫/막차 | %s ~ %s", earliest.format(fmt), latest.format(fmt));
    }

    @Transactional
    public List<kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule> getTargetRules() {
        List<kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule> rules = busTargetRuleRepository.findAll();

        if (rules.isEmpty()) {
            // 기본 룰 자동 등록 (시종점 페어 기반 - 웹 서비스 시절 100% 동일 정류소 완벽 복원)
            List<kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule> defaults = List.of(
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule.builder()
                            .category("go-school").tabName("인입런")
                            .startBstopId("164000395").startStopName("인천대입구역 2번출구").startStopAlias("인입")
                            .endBstopId("164000375, 164000499, 164000386, 164000763")
                            .endBstopName("인천대 자연과학대학, 인천대 공과대학, 인천대 정문(앞), 인천대 송도캠퍼스(기숙사)")
                            .endStopAlias("자연대, 공대, 정문, 기숙사")
                            .targetKeywords("자연대,공대,정문,기숙사,송도캠퍼스,인천대").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule.builder()
                            .category("go-school").tabName("인입런")
                            .startBstopId("164000648").startStopName("인천대입구역.롯데몰").startStopAlias("인입")
                            .endBstopId("164000375, 164000499, 164000386, 164000751")
                            .endBstopName("인천대 자연과학대학, 인천대 공과대학, 인천대 정문(앞), 인천대 송도캠퍼스(기숙사)")
                            .endStopAlias("자연대, 공대, 정문, 기숙사")
                            .targetKeywords("자연대,공대,정문,기숙사,송도캠퍼스,인천대").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule.builder()
                            .category("go-school").tabName("인입런")
                            .startBstopId("164000396").startStopName("인천대입구역 1번출구").startStopAlias("인입")
                            .endBstopId("164000375, 164000499, 164000386, 164000751")
                            .endBstopName("인천대 자연과학대학, 인천대 공과대학, 인천대 정문(앞), 인천대 송도캠퍼스(기숙사)")
                            .endStopAlias("자연대, 공대, 정문, 기숙사")
                            .targetKeywords("자연대,공대,정문,기숙사,송도캠퍼스,인천대").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule.builder()
                            .category("go-school").tabName("지정단런")
                            .startBstopId("164000403").startStopName("지식정보단지역 3번출구").startStopAlias("지정단")
                            .endBstopId("164000376, 164000375")
                            .endBstopName("인천대 공과대학, 인천대 자연과학대학")
                            .endStopAlias("공대, 자연대")
                            .targetKeywords("공대,자연대,인천대").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule.builder()
                            .category("go-home").tabName("인천대 정문")
                            .startBstopId("164000385").startStopName("인천대 정문(길 건너)").startStopAlias("정문")
                            .endBstopId("164000396").endBstopName("인천대입구역 1번출구").endStopAlias("인입")
                            .targetKeywords("인입,인천대입구역").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule.builder()
                            .category("go-home").tabName("인천대 정문")
                            .startBstopId("164000386").startStopName("인천대 정문(앞)").startStopAlias("정문")
                            .endBstopId("164000395").endBstopName("인천대입구역 2번출구").endStopAlias("인입")
                            .targetKeywords("인입,인천대입구역").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule.builder()
                            .category("go-home").tabName("공대/자연대")
                            .startBstopId("164000378").startStopName("인천대 자연과학대학").startStopAlias("자연대")
                            .endBstopId("164000396, 164000404, 164000380")
                            .endBstopName("인천대입구역 1번출구, 지식정보단지역 2번출구, 지식정보단지역")
                            .endStopAlias("인입, 지정단, 지정단")
                            .targetKeywords("인입,지정단,지식정보단지,인천대입구역").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule.builder()
                            .category("go-home").tabName("공대/자연대")
                            .startBstopId("164000377").startStopName("인천대 공과대학").startStopAlias("공대")
                            .endBstopId("164000396, 164000404, 164000380")
                            .endBstopName("인천대입구역 1번출구, 지식정보단지역 2번출구, 지식정보단지역")
                            .endStopAlias("인입, 지정단, 지정단")
                            .targetKeywords("인입,지정단,지식정보단지,인천대입구역").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule.builder()
                            .category("go-home").tabName("기숙사 앞")
                            .startBstopId("164000751").startStopName("인천대 송도캠퍼스(기숙사)").startStopAlias("기숙사")
                            .endBstopId("164000396, 164000395")
                            .endBstopName("인천대입구역 1번출구, 인천대입구역 2번출구")
                            .endStopAlias("인입, 인입")
                            .targetKeywords("인입,인천대입구역").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule.builder()
                            .category("go-home").tabName("1301번")
                            .startBstopId("164000378").startStopName("인천대 자연과학대학").startStopAlias("자연대")
                            .endBstopId("164000349").endBstopName("송도풍림아이원1단지(금호아파트)").endStopAlias("금호아파트")
                            .targetKeywords("금호,풍림,홍대").build(),
                    kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule.builder()
                            .category("go-home").tabName("1301번")
                            .startBstopId("164000377").startStopName("인천대 공과대학").startStopAlias("공대")
                            .endBstopId("164000034").endBstopName("송도풍림아이원2단지").endStopAlias("풍림2단지")
                            .targetKeywords("금호,풍림,홍대").build()
            );

            busTargetRuleRepository.saveAll(defaults);
            rules = busTargetRuleRepository.findAll();
        }
        return rules;
    }

    @Transactional
    public kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule saveTargetRule(BusTargetRuleDto dto) {
        kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule rule;
        if (dto.getId() != null) {
            rule = busTargetRuleRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 룰입니다. ID=" + dto.getId()));
            rule.update(
                    dto.getCategory(),
                    dto.getTabName(),
                    dto.getStartBstopId(),
                    dto.getStartStopName(),
                    dto.getStartStopAlias(),
                    dto.getEndBstopId(),
                    dto.getEndBstopName(),
                    dto.getEndStopAlias(),
                    dto.getTargetKeywords()
            );
        } else {
            rule = kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule.builder()
                    .category(dto.getCategory())
                    .tabName(dto.getTabName())
                    .startBstopId(dto.getStartBstopId())
                    .startStopName(dto.getStartStopName())
                    .startStopAlias(dto.getStartStopAlias())
                    .endBstopId(dto.getEndBstopId())
                    .endBstopName(dto.getEndBstopName())
                    .endStopAlias(dto.getEndStopAlias())
                    .targetKeywords(dto.getTargetKeywords())
                    .build();
            rule = busTargetRuleRepository.save(rule);
        }

        // 정류소 및 별칭 동기화
        if (dto.getStartBstopId() != null && !busTargetStopRepository.existsByBstopId(dto.getStartBstopId())) {
            addTargetStop(TargetStopRequestDto.builder()
                    .bstopId(dto.getStartBstopId())
                    .bstopName(dto.getStartStopName())
                    .stopAlias(dto.getStartStopAlias())
                    .category(dto.getTabName())
                    .build());
        }

        // 별칭 사전에도 자동 등록 (없는 경우)
        if (dto.getStartBstopId() != null && dto.getStartStopAlias() != null && !dto.getStartStopAlias().isBlank()) {
            saveStopAlias(BusStopAliasDto.builder()
                    .bstopId(dto.getStartBstopId())
                    .bstopName(dto.getStartStopName())
                    .stopAlias(dto.getStartStopAlias())
                    .memo(dto.getTabName() + " 출발 정류장")
                    .build());
        }
        if (dto.getEndBstopId() != null && dto.getEndStopAlias() != null && !dto.getEndStopAlias().isBlank()) {
            saveStopAlias(BusStopAliasDto.builder()
                    .bstopId(dto.getEndBstopId())
                    .bstopName(dto.getEndBstopName())
                    .stopAlias(dto.getEndStopAlias())
                    .memo(dto.getTabName() + " 도착 정류장")
                    .build());
        }

        return rule;
    }

    @Transactional
    public kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule updateTargetRule(Long id, BusTargetRuleDto dto) {
        kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule rule = busTargetRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 규칙 ID입니다: " + id));

        rule.update(
                dto.getCategory(),
                dto.getTabName(),
                dto.getStartBstopId(),
                dto.getStartStopName(),
                dto.getStartStopAlias(),
                dto.getEndBstopId(),
                dto.getEndBstopName(),
                dto.getEndStopAlias(),
                dto.getTargetKeywords()
        );

        // 정류소 및 별칭 동기화
        if (dto.getStartBstopId() != null && !busTargetStopRepository.existsByBstopId(dto.getStartBstopId())) {
            addTargetStop(TargetStopRequestDto.builder()
                    .bstopId(dto.getStartBstopId())
                    .bstopName(dto.getStartStopName())
                    .stopAlias(dto.getStartStopAlias())
                    .category(dto.getTabName())
                    .build());
        }
        if (dto.getStartBstopId() != null && dto.getStartStopAlias() != null && !dto.getStartStopAlias().isBlank()) {
            saveStopAlias(BusStopAliasDto.builder()
                    .bstopId(dto.getStartBstopId())
                    .bstopName(dto.getStartStopName())
                    .stopAlias(dto.getStartStopAlias())
                    .memo(dto.getTabName() + " 출발 정류장")
                    .build());
        }
        if (dto.getEndBstopId() != null && dto.getEndStopAlias() != null && !dto.getEndStopAlias().isBlank()) {
            saveStopAlias(BusStopAliasDto.builder()
                    .bstopId(dto.getEndBstopId())
                    .bstopName(dto.getEndBstopName())
                    .stopAlias(dto.getEndStopAlias())
                    .memo(dto.getTabName() + " 도착 정류장")
                    .build());
        }

        return rule;
    }

    @Transactional
    public void deleteTargetRule(Long id) {
        busTargetRuleRepository.deleteById(id);
    }


    @Transactional
    public kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule addTargetRule(BusTargetRuleDto dto) {
        return saveTargetRule(dto);
    }

    @Transactional
    public List<BusRouteSectionResponseDto> autoSyncRoutes() {
        List<kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule> rules = getTargetRules();
        List<BusRouteSectionResponseDto> syncedResults = new ArrayList<>();

        for (kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetRule rule : rules) {
            String category = rule.getCategory();
            String tabName = rule.getTabName();
            String startBstopId = rule.getStartBstopId();
            String startStopName = rule.getStartStopName();
            String endBstopId = rule.getEndBstopId();
            String endStopName = rule.getEndBstopName();

            // 수집 대상 정류소 자동 활성화 확인
            if (!busTargetStopRepository.existsByBstopId(startBstopId)) {
                addTargetStop(TargetStopRequestDto.builder()
                        .bstopId(startBstopId)
                        .bstopName(startStopName)
                        .category(tabName)
                        .build());
            }

            // 정류소를 경유하는 모든 인가 노선 목록 탐색 (실시간 운행 여부와 무관하게 24시간 항상 탐색 가능)
            Map<String, String> candidateRoutes = new HashMap<>(); // routeId -> routeNo

            List<kr.inuappcenterportal.inuportal.domain.bus.dto.BusRouteInfoItemDto> viaRoutes = busApiService.fetchRoutesViaStation(startBstopId);
            for (kr.inuappcenterportal.inuportal.domain.bus.dto.BusRouteInfoItemDto vr : viaRoutes) {
                if (vr.getRouteId() != null && !vr.getRouteId().isBlank()) {
                    candidateRoutes.put(vr.getRouteId(), vr.getRouteNo());
                }
            }

            // 실시간 도착 API에서도 추가 보완
            List<BusArrivalItemDto> arrivals = busApiService.fetchBusArrivals(startBstopId);
            for (BusArrivalItemDto arrival : arrivals) {
                if (arrival.getRouteId() != null && !arrival.getRouteId().isBlank()) {
                    candidateRoutes.putIfAbsent(arrival.getRouteId(), arrival.getRouteNo());
                }
            }

            for (Map.Entry<String, String> entry : candidateRoutes.entrySet()) {
                String routeId = entry.getKey();
                String routeNo = entry.getValue();

                if (routeId == null || routeId.isBlank() || routeNo == null) {
                    continue;
                }

                // 공공데이터포털 초당 요청 수(QPS) 제한 초과 방지 및 안정적인 동기화를 위한 충분한 딜레이 (400ms)
                try {
                    Thread.sleep(400);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }

                List<BusRouteStopDto> allStops = busApiService.fetchRouteStops(routeId);
                if (allStops.isEmpty()) {
                    continue;
                }

                // 1. 시작 정류장 후보 인덱스 찾기 (bstopId 정확 일치 우선)
                List<Integer> startCandidates = new ArrayList<>();
                for (int i = 0; i < allStops.size(); i++) {
                    BusRouteStopDto s = allStops.get(i);
                    if (startBstopId != null && !startBstopId.isBlank() && startBstopId.equals(s.getBstopId())) {
                        startCandidates.add(i);
                    }
                }
                // bstopId가 입력되지 않은 경우에만 명칭 일치로 fallback
                if (startCandidates.isEmpty() && (startBstopId == null || startBstopId.isBlank()) && startStopName != null && !startStopName.isBlank()) {
                    for (int i = 0; i < allStops.size(); i++) {
                        BusRouteStopDto s = allStops.get(i);
                        if (s.getBstopName() != null && s.getBstopName().contains(startStopName)) {
                            startCandidates.add(i);
                        }
                    }
                }

                if (startCandidates.isEmpty()) {
                    continue;
                }

                // 2. 목표 도착 정류장 후보 인덱스 찾기 (ID가 등록된 경우 ID로만 엄격 매칭)
                Map<Integer, String> endCandidates = new HashMap<>(); // index -> bstopName
                if (endBstopId != null && !endBstopId.isBlank()) {
                    List<String> targetIds = Arrays.stream(endBstopId.split(","))
                            .map(String::trim)
                            .filter(id -> !id.isBlank())
                            .collect(Collectors.toList());

                    for (int i = 0; i < allStops.size(); i++) {
                        BusRouteStopDto s = allStops.get(i);
                        if (s.getBstopId() != null && targetIds.contains(s.getBstopId())) {
                            endCandidates.put(i, s.getBstopName());
                        }
                    }
                } else if (endStopName != null && !endStopName.isBlank()) {
                    List<String> targetNames = Arrays.stream(endStopName.split(","))
                            .map(String::trim)
                            .filter(n -> !n.isBlank())
                            .collect(Collectors.toList());

                    for (int i = 0; i < allStops.size(); i++) {
                        BusRouteStopDto s = allStops.get(i);
                        if (s.getBstopName() != null) {
                            for (String tn : targetNames) {
                                if (s.getBstopName().contains(tn)) {
                                    endCandidates.put(i, s.getBstopName());
                                    break;
                                }
                            }
                        }
                    }
                }

                // 3. 최적 정방향 유효 구간(startIdx < endIdx, 15정거장 이내) 선택
                int bestStartIdx = -1;
                int bestEndIdx = -1;
                int minDistance = Integer.MAX_VALUE;
                String matchedEndStopName = endStopName;

                for (int sIdx : startCandidates) {
                    for (Map.Entry<Integer, String> eEntry : endCandidates.entrySet()) {
                        int eIdx = eEntry.getKey();
                        if (eIdx > sIdx) {
                            int dist = eIdx - sIdx;
                            // 관리자가 지정한 목적지 중 정방향 최단 유효 경로 선택 (1~15 정거장 이내)
                            if (dist <= 15 && dist < minDistance) {
                                minDistance = dist;
                                bestStartIdx = sIdx;
                                bestEndIdx = eIdx;
                                matchedEndStopName = eEntry.getValue();
                            }
                        }
                    }
                }

                // 정방향으로 유효한 구간을 찾은 경우에만 등록
                int startIdx = bestStartIdx;
                int endIdx = bestEndIdx;
                if (endIdx != -1 && endIdx > startIdx) {
                    String sectionName = String.format("%s - %s번", tabName, routeNo);
                    List<BusRouteStopDto> slicedDto = allStops.subList(startIdx, endIdx + 1);

                    // 기존 등록된 구간이 있는지 확인 (출발 정류소 ID까지 포함하여 복합 식별)
                    Optional<BusRouteSection> existingOpt = busRouteSectionRepository
                            .findByRouteNoAndCategoryAndTabNameAndStartBstopId(routeNo, category, tabName, startBstopId);

                    String busNotice = existingOpt.map(BusRouteSection::getBusNotice).orElse(null);
                    String routeNotice = existingOpt.map(BusRouteSection::getRouteNotice).orElse(null);
                    if (busNotice == null || busNotice.isBlank()) {
                        String allocGap = busApiService.fetchAllocGap(routeId);
                        if (allocGap != null) busNotice = allocGap;
                    }

                    // 과거 웹 서비스 꿀팁 문구 기본 복원
                    if (routeNotice == null || routeNotice.isBlank()) {
                        if ("8".equals(routeNo) && "go-home".equals(category)) {
                            routeNotice = "공대 → 자연대 → 정문 → 인입";
                        } else if ("6-1".equals(routeNo) && "go-home".equals(category)) {
                            routeNotice = "공대 → 자연대 → 지정단 2번출구";
                        } else if ("6".equals(routeNo) && "go-home".equals(category)) {
                            routeNotice = "많이 돌아가는 노선이니 주의하세요!";
                        } else if ("1301".equals(routeNo) && "go-home".equals(category)) {
                            routeNotice = "공대/자연대 → 인천논현 → 부천시청 → 홍대입구";
                        }
                    }

                    final String finalEndStopName = matchedEndStopName;
                    final String finalBusNotice = busNotice;
                    final String finalRouteNotice = routeNotice;

                    BusRouteSection section = existingOpt.orElseGet(() -> BusRouteSection.builder()
                            .sectionName(sectionName)
                            .category(category)
                            .tabName(tabName)
                            .routeNo(routeNo)
                            .routeId(routeId)
                            .startBstopId(startBstopId)
                            .startBstopName(startStopName)
                            .endBstopId(endBstopId)
                            .endBstopName(finalEndStopName)
                            .busNotice(finalBusNotice)
                            .routeNotice(finalRouteNotice)
                            .build());

                    section.updateSectionInfo(sectionName, category, tabName, finalBusNotice, finalRouteNotice);



                    List<BusRouteStop> routeStops = new ArrayList<>();
                    int seq = 1;
                    for (BusRouteStopDto stopDto : slicedDto) {
                        routeStops.add(BusRouteStop.builder()
                                .seq(seq++)
                                .bstopId(stopDto.getBstopId())
                                .bstopName(stopDto.getBstopName())
                                .latitude(stopDto.getLatitude())
                                .longitude(stopDto.getLongitude())
                                .build());
                    }

                    section.updateStops(routeStops);
                    BusRouteSection saved = busRouteSectionRepository.save(section);
                    syncedResults.add(BusRouteSectionResponseDto.from(saved));
                }
            }
        }

        return syncedResults;
    }

    @Transactional
    public BusRouteSectionResponseDto updateRouteSection(Long id, RouteSectionUpdateRequest request) {
        BusRouteSection section = busRouteSectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 노선 구간 ID입니다: " + id));

        section.updateSectionInfo(
                request.getSectionName(),
                request.getCategory(),
                request.getTabName(),
                request.getBusNotice(),
                request.getRouteNotice()
        );

        return BusRouteSectionResponseDto.from(section);
    }


    @Transactional
    public void deleteRouteSection(Long id) {


        busRouteSectionRepository.deleteById(id);
    }

    @Transactional
    public BusRouteSectionResponseDto createOrUpdateRouteSection(RouteSectionCreateRequest request) {

        Optional<BusRouteSection> existingOpt = busRouteSectionRepository
                .findByRouteNoAndCategoryAndTabName(request.getRouteNo(), request.getCategory(), request.getTabName());

        BusRouteSection section = existingOpt.orElseGet(() -> BusRouteSection.builder()
                .sectionName(request.getSectionName())
                .category(request.getCategory())
                .tabName(request.getTabName())
                .routeNo(request.getRouteNo())
                .startBstopName(request.getStartStop())
                .endBstopName(request.getEndStop())
                .busNotice(request.getBusNotice())
                .routeNotice(request.getRouteNotice())
                .build());

        // 공공데이터포털에서 노선 경유 정류장 목록 가져와서 슬라이싱

        if (section.getRouteId() != null && !section.getRouteId().isBlank()) {
            List<BusRouteStopDto> allStops = busApiService.fetchRouteStops(section.getRouteId());
            List<BusRouteStop> slicedStops = sliceStopsByStartAndEnd(allStops, request.getStartStop(), request.getEndStop());
            section.updateStops(slicedStops);
        }

        BusRouteSection saved = busRouteSectionRepository.save(section);
        return BusRouteSectionResponseDto.from(saved);
    }

    @Transactional
    public BusTargetStop addTargetStop(TargetStopRequestDto request) {
        BusTargetStop targetStop = busTargetStopRepository.findByBstopId(request.getBstopId())
                .orElseGet(() -> BusTargetStop.builder()
                        .bstopId(request.getBstopId())
                        .bstopName(request.getBstopName())
                        .stopAlias(request.getStopAlias())
                        .category(request.getCategory())
                        .isActive(true)
                        .build());
        return busTargetStopRepository.save(targetStop);
    }


    public List<BusTargetStop> getTargetStops() {
        return busTargetStopRepository.findByIsActiveTrue();
    }

    private List<BusArrivalItemDto> calculateEstimatedArrivals(String bstopId) {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek currentDay = now.getDayOfWeek();
        LocalTime currentTime = now.toLocalTime();

        // 최근 4주간 동 요일, 동 시간대(±20분) 도착 기록 수집
        List<Integer> estimatedTimes = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            LocalDate pastDate = now.toLocalDate().minusWeeks(i);
            LocalDateTime startWindow = pastDate.atTime(currentTime.minusMinutes(20));
            LocalDateTime endWindow = pastDate.atTime(currentTime.plusMinutes(20));

            List<BusArrivalHistory> pastLogs = busArrivalHistoryRepository
                    .findByBstopIdAndCreateDateBetweenOrderByCreateDateAsc(bstopId, startWindow, endWindow);

            for (BusArrivalHistory log : pastLogs) {
                if (log.getArrivalEstimateTime() != null && log.getArrivalEstimateTime() > 0) {
                    estimatedTimes.add(log.getArrivalEstimateTime());
                }
            }
        }

        if (estimatedTimes.isEmpty()) {
            return List.of();
        }

        // 중앙값 계산
        Collections.sort(estimatedTimes);
        int medianSeconds = estimatedTimes.get(estimatedTimes.size() / 2);

        String notice = String.format("통계 기반 추정: 약 %d분 후 도착 예상 (최근 4주 %s 요일 데이터 기준)",
                (medianSeconds + 59) / 60, currentDay.name());

        return List.of(BusArrivalItemDto.builder()
                .bstopId(bstopId)
                .estimatedArrivalSeconds(medianSeconds)
                .estimationNotice(notice)
                .build());
    }

    private Integer calculateAverageIntervalForDayOfWeek(String bstopId, DayOfWeek dayOfWeek) {
        LocalDate today = LocalDate.now();
        List<Long> intervals = new ArrayList<>();

        for (int i = 1; i <= 4; i++) {
            LocalDate pastDate = today.minusWeeks(i);
            while (pastDate.getDayOfWeek() != dayOfWeek) {
                pastDate = pastDate.minusDays(1);
            }

            LocalDateTime startOfDay = pastDate.atStartOfDay();
            LocalDateTime endOfDay = pastDate.atTime(LocalTime.MAX);

            List<BusArrivalHistory> logs = busArrivalHistoryRepository
                    .findByBstopIdAndCreateDateBetweenOrderByCreateDateAsc(bstopId, startOfDay, endOfDay);

            for (int j = 1; j < logs.size(); j++) {
                LocalDateTime prevTime = logs.get(j - 1).getCreateDate();
                LocalDateTime currTime = logs.get(j).getCreateDate();
                long diffSeconds = java.time.Duration.between(prevTime, currTime).getSeconds();
                if (diffSeconds > 120 && diffSeconds < 3600) { // 2분 ~ 60분 간격
                    intervals.add(diffSeconds);
                }
            }
        }

        if (intervals.isEmpty()) {
            return null;
        }

        double avg = intervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
        return (int) avg;
    }

    private List<BusRouteStop> sliceStopsByStartAndEnd(List<BusRouteStopDto> allStops, String startStop, String endStop) {
        if (allStops == null || allStops.isEmpty()) {
            return List.of();
        }

        List<Integer> startCandidates = new ArrayList<>();
        if (startStop != null && !startStop.isBlank()) {
            for (int i = 0; i < allStops.size(); i++) {
                BusRouteStopDto stop = allStops.get(i);
                if (stop.getBstopId() != null && stop.getBstopId().equals(startStop)) {
                    startCandidates.add(i);
                }
            }
            if (startCandidates.isEmpty()) {
                for (int i = 0; i < allStops.size(); i++) {
                    BusRouteStopDto stop = allStops.get(i);
                    if (stop.getBstopName() != null && stop.getBstopName().contains(startStop)) {
                        startCandidates.add(i);
                    }
                }
            }
        } else {
            startCandidates.add(0);
        }

        List<Integer> endCandidates = new ArrayList<>();
        if (endStop != null && !endStop.isBlank()) {
            for (int i = 0; i < allStops.size(); i++) {
                BusRouteStopDto stop = allStops.get(i);
                if (stop.getBstopId() != null && stop.getBstopId().equals(endStop)) {
                    endCandidates.add(i);
                }
            }
            if (endCandidates.isEmpty()) {
                for (int i = 0; i < allStops.size(); i++) {
                    BusRouteStopDto stop = allStops.get(i);
                    if (stop.getBstopName() != null && stop.getBstopName().contains(endStop)) {
                        endCandidates.add(i);
                    }
                }
            }
        } else {
            endCandidates.add(allStops.size() - 1);
        }

        int bestStart = 0;
        int bestEnd = allStops.size() - 1;
        int minDistance = Integer.MAX_VALUE;

        for (int sIdx : startCandidates) {
            for (int eIdx : endCandidates) {
                if (eIdx > sIdx) {
                    int dist = eIdx - sIdx;
                    if (dist < minDistance) {
                        minDistance = dist;
                        bestStart = sIdx;
                        bestEnd = eIdx;
                    }
                }
            }
        }

        List<BusRouteStop> sliced = new ArrayList<>();
        int seq = 1;
        for (int i = bestStart; i <= bestEnd && i < allStops.size(); i++) {
            BusRouteStopDto dto = allStops.get(i);
            sliced.add(BusRouteStop.builder()
                    .seq(seq++)
                    .bstopId(dto.getBstopId())
                    .bstopName(dto.getBstopName())
                    .latitude(dto.getLatitude())
                    .longitude(dto.getLongitude())
                    .build());
        }
        return sliced;
    }
}
