package kr.inuappcenterportal.inuportal.domain.bus.service;

import kr.inuappcenterportal.inuportal.domain.bus.dto.BusArrivalItemDto;
import kr.inuappcenterportal.inuportal.domain.bus.entity.BusArrivalHistory;
import kr.inuappcenterportal.inuportal.domain.bus.entity.BusRouteSection;
import kr.inuappcenterportal.inuportal.domain.bus.repository.BusArrivalHistoryRepository;
import kr.inuappcenterportal.inuportal.domain.bus.repository.BusRouteSectionRepository;
import kr.inuappcenterportal.inuportal.global.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BusServiceEstimationTest {

    @Mock
    private BusApiService busApiService;
    @Mock
    private BusArrivalHistoryRepository busArrivalHistoryRepository;
    @Mock
    private BusRouteSectionRepository busRouteSectionRepository;
    @Mock
    private RedisService redisService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BusService busService;

    @Test
    @DisplayName("과거 4주 실측 이력이 존재할 때 과거 기록 기반 예측치가 성공적으로 산출된다")
    void testAugmentWithEstimatedArrivals_success() {
        // given
        String bstopId = "164000395";
        String routeId = "165000012";
        String routeNo = "8";

        BusRouteSection section = BusRouteSection.builder()
                .startBstopId(bstopId)
                .routeId(routeId)
                .routeNo(routeNo)
                .build();
        given(busRouteSectionRepository.findByStartBstopId(bstopId)).willReturn(List.of(section));
        given(redisService.getValue(anyString())).willReturn(null);

        given(busArrivalHistoryRepository.findFirstByBstopIdAndRouteIdAndCreateDateBetweenOrderByCreateDateDesc(eq(bstopId), eq(routeId), any(), any()))
                .willReturn(Optional.empty());
        given(busArrivalHistoryRepository.findFirstByBstopIdAndRouteIdAndCreateDateBetweenOrderByCreateDateAsc(eq(bstopId), eq(routeId), any(), any()))
                .willAnswer(invocation -> {
                    LocalDateTime startWindow = invocation.getArgument(2);
                    BusArrivalHistory h = BusArrivalHistory.builder().bstopId(bstopId).routeId(routeId).build();
                    org.springframework.test.util.ReflectionTestUtils.setField(h, "createDate", startWindow.plusMinutes(10));
                    return Optional.of(h);
                });

        // when
        List<BusArrivalItemDto> result = busService.augmentWithEstimatedArrivals(bstopId, List.of());

        // then
        assertThat(result).hasSize(1);
        BusArrivalItemDto estimated = result.get(0);
        assertThat(estimated.getRouteId()).isEqualTo(routeId);
        assertThat(estimated.getLatestStopName()).isEqualTo("과거 기록 기반");
        assertThat(estimated.getEstimatedArrivalSeconds()).isGreaterThan(0);
    }
}
