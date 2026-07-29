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

    // TODO: API 키 갱신 후 true로 변경하여 공공데이터포털 연동 재개 가능
    private static final boolean IS_API_ENABLED = false;

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
        if (!IS_API_ENABLED) {
            log.trace("공공데이터 버스 API 수집이 일시 중단된 상태입니다.");
            return;
        }


        List<BusTargetStop> activeStops = busTargetStopRepository.findByIsActiveTrue();

        if (activeStops.isEmpty()) {
            return;
        }

        log.debug("30초 주기 버스 도착 정보 수집 시작 (대상 정류장 수: {})", activeStops.size());

        for (BusTargetStop stop : activeStops) {
            try {
                List<BusArrivalItemDto> arrivals = busApiService.fetchBusArrivals(stop.getBstopId());
                for (BusArrivalItemDto arrival : arrivals) {
                    Integer estimateTime = parseIntegerSafe(arrival.getArrivalEstimateTime());
                    Integer restStops = parseIntegerSafe(arrival.getRestStopCount());

                    BusArrivalHistory history = BusArrivalHistory.builder()
                            .bstopId(stop.getBstopId())
                            .bstopName(stop.getBstopName())
                            .routeId(arrival.getRouteId())
                            .routeNo(arrival.getRouteNo())
                            .busId(arrival.getBusId())
                            .busNumPlate(arrival.getBusNumPlate())
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
     * 매일 새벽 4시, 30일 경과한 과거 버스 도착 기록 정리
     */
    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(
            name = "bus-history-cleanup",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT1M"
    )
    @Transactional
    public void cleanupOldArrivalHistory() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        int deletedCount = busArrivalHistoryRepository.deleteByCreateDateBefore(cutoffDate);
        log.info("30일 초과 과거 버스 도착 기록 {}건 삭제 완료", deletedCount);
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
