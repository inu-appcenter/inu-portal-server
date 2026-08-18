package kr.inuappcenterportal.inuportal.domain.bus.service;

import kr.inuappcenterportal.inuportal.domain.bus.dto.BusArrivalItemDto;
import kr.inuappcenterportal.inuportal.domain.bus.entity.BusArrivalHistory;
import kr.inuappcenterportal.inuportal.domain.bus.entity.BusTargetStop;
import kr.inuappcenterportal.inuportal.domain.bus.repository.BusArrivalHistoryRepository;
import kr.inuappcenterportal.inuportal.domain.bus.repository.BusTargetStopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusScheduler {

    private final BusApiService busApiService;
    private final BusTargetStopRepository busTargetStopRepository;
    private final BusArrivalHistoryRepository busArrivalHistoryRepository;
    private final kr.inuappcenterportal.inuportal.global.service.RedisService redisService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final BusService busService;

    /**
     * 30초마다 관리 대상 정류장의 실시간 버스 도착 정보를 DB에 수집
     */
    @Scheduled(fixedRate = 30000)
    @SchedulerLock(
            name = "bus-arrival-polling",
            lockAtMostFor = "PT25S",
            lockAtLeastFor = "PT5S"
    )
    @Transactional
    public void pollTargetStopsArrivalData() {
        if (!busService.isBusServiceEnabled()) {
            log.trace("DB 설정에 의해 버스 실시간 도착 정보 수집 및 API 호출이 비활성화(OFF)되어 있습니다.");
            return;
        }

        // 운행 시간대(05:00~24:00)에만 수집
        int currentHour = java.time.LocalTime.now().getHour();
        if (currentHour >= 0 && currentHour < 5) {
            return;
        }

        List<BusTargetStop> activeStops = busTargetStopRepository.findByIsActiveTrue();

        if (activeStops.isEmpty()) {
            return;
        }

        log.debug("30초 주기 버스 도착 정보 수집 시작 (대상 정류장 수: {})", activeStops.size());

        for (BusTargetStop stop : activeStops) {
            try {
                // 공공데이터포털 초당 트래픽(TPS) 제한 초과 방지를 위한 짧은 딜레이 (300ms)
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }

                List<BusArrivalItemDto> arrivals = busApiService.fetchBusArrivals(stop.getBstopId());
                
                // 조회된 실시간 데이터를 Redis에 캐싱 (스케줄러 주기가 30초이므로 여유있게 40초 TTL 설정)
                try {
                    String json = objectMapper.writeValueAsString(arrivals);
                    redisService.storeValueWithExpire("bus_realtime:" + stop.getBstopId(), json, 40, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception ex) {
                    log.error("Redis 캐싱 실패 - bstopId: {}", stop.getBstopId(), ex);
                }

                LocalDateTime sevenMinutesAgo = LocalDateTime.now().minusMinutes(7);

                for (BusArrivalItemDto arrival : arrivals) {
                    Integer estimateTime = parseIntegerSafe(arrival.getArrivalEstimateTime());
                    Integer restStops = parseIntegerSafe(arrival.getRestStopCount());

                    // 실제 해당 정류소에 "도착(도착 임박/1정거장 전 이하)"한 이벤트만 수집
                    boolean isArriving = (restStops != null && restStops <= 1)
                            || (estimateTime != null && estimateTime <= 90);

                    if (!isArriving) {
                        continue;
                    }

                    String busNumPlate = arrival.getBusNumPlate();
                    String routeId = arrival.getRouteId();

                    // 최근 7분 이내에 동일 정류장/노선/차량의 도착 기록이 이미 있으면 중복 저장 방지
                    if (busNumPlate != null && !busNumPlate.isBlank()) {
                        boolean existsRecent = busArrivalHistoryRepository
                                .existsByBstopIdAndRouteIdAndBusNumPlateAndCreateDateAfter(
                                        stop.getBstopId(), routeId, busNumPlate, sevenMinutesAgo);
                        if (existsRecent) {
                            continue;
                        }
                    }

                    BusArrivalHistory history = BusArrivalHistory.builder()
                            .bstopId(stop.getBstopId())
                            .bstopName(stop.getBstopName())
                            .routeId(routeId)
                            .routeNo(arrival.getRouteNo())
                            .busId(arrival.getBusId())
                            .busNumPlate(busNumPlate)
                            .arrivalEstimateTime(estimateTime)
                            .restStopCount(restStops)
                            .congestion(arrival.getCongestion())
                            .build();

                    busArrivalHistoryRepository.save(history);
                }
            } catch (Exception e) {
                log.error("정류장({}) 도착 정보 수집 실패", stop.getBstopId(), e);
            }
        }
    }

    /**
     * 매일 새벽 4시, 4주(28일) 경과한 과거 버스 도착 기록 정리
     */
    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(
            name = "bus-history-cleanup",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT1M"
    )
    @Transactional
    public void cleanupOldArrivalHistory() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusWeeks(4);
        int deletedCount = busArrivalHistoryRepository.deleteByCreateDateBefore(cutoffDate);
        log.info("4주(28일) 초과 과거 버스 도착 기록 {}건 삭제 완료", deletedCount);
    }

    private Integer parseIntegerSafe(String val) {
        if (val == null || val.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
