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

    public List<BusArrivalItemDto> getRealtimeArrivals(String bstopId) {
        List<BusArrivalItemDto> arrivals = busApiService.fetchBusArrivals(bstopId);

        if (arrivals.isEmpty()) {
            // 실시간 정보가 없는 경우 통계 기반 추정치 추가
            return calculateEstimatedArrivals(bstopId);
        }

        return arrivals;
    }

    public BusHistoryResponseDto getHistory(String bstopId, String targetDateStr) {
        LocalDate targetDate = (targetDateStr != null && !targetDateStr.isBlank())
                ? LocalDate.parse(targetDateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                : LocalDate.now();

        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(LocalTime.MAX);

        List<BusArrivalHistory> historyList = busArrivalHistoryRepository
                .findByBstopIdAndCreateDateBetweenOrderByCreateDateAsc(bstopId, startOfDay, endOfDay);

        List<BusHistoryResponseDto.HistoryRecord> records = historyList.stream()
                .map(h -> BusHistoryResponseDto.HistoryRecord.builder()
                        .id(h.getId())
                        .routeId(h.getRouteId())
                        .routeNo(h.getRouteNo())
                        .busNumPlate(h.getBusNumPlate())
                        .arrivalEstimateTime(h.getArrivalEstimateTime())
                        .restStopCount(h.getRestStopCount())
                        .arrivalTime(h.getCreateDate())
                        .build())
                .collect(Collectors.toList());

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
                .map(BusRouteSectionResponseDto::from)
                .collect(Collectors.toList());
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

        int startIndex = 0;
        int endIndex = allStops.size() - 1;

        if (startStop != null && !startStop.isBlank()) {
            for (int i = 0; i < allStops.size(); i++) {
                BusRouteStopDto stop = allStops.get(i);
                if (stop.getBstopId().equals(startStop) || stop.getBstopName().contains(startStop)) {
                    startIndex = i;
                    break;
                }
            }
        }

        if (endStop != null && !endStop.isBlank()) {
            for (int i = startIndex; i < allStops.size(); i++) {
                BusRouteStopDto stop = allStops.get(i);
                if (stop.getBstopId().equals(endStop) || stop.getBstopName().contains(endStop)) {
                    endIndex = i;
                    break;
                }
            }
        }

        List<BusRouteStop> sliced = new ArrayList<>();
        int seq = 1;
        for (int i = startIndex; i <= endIndex && i < allStops.size(); i++) {
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
