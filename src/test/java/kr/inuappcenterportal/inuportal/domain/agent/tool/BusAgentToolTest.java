package kr.inuappcenterportal.inuportal.domain.agent.tool;

import kr.inuappcenterportal.inuportal.domain.agent.tool.impl.BusAgentTool;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusArrivalItemDto;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusHistoryResponseDto;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusRouteSectionResponseDto;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopAliasDto;
import kr.inuappcenterportal.inuportal.domain.bus.service.BusService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BusAgentToolTest {

    @Mock
    private BusService busService;

    @InjectMocks
    private BusAgentTool busAgentTool;

    @Test
    @DisplayName("'2번 출구 버스' 질의 시 '인천대입구역 2번출구' 노선 섹션과 매칭되어 도착 정보가 조회된다")
    void executeWithExit2MatchesIncheonStationExit2() {
        BusStopAliasDto alias = BusStopAliasDto.builder()
                .bstopId("38391")
                .stopAlias("인천대입구역 2번출구")
                .build();

        BusRouteSectionResponseDto section = BusRouteSectionResponseDto.builder()
                .id(1L)
                .category("go-school")
                .tabName("인입런")
                .startBstopId("38391")
                .startBstopName("인천대입구역")
                .startBstopAlias("인천대입구역 2번출구")
                .routeNo("순환46")
                .build();

        given(busService.getStopAliases()).willReturn(List.of(alias));
        given(busService.getRouteSections(null)).willReturn(List.of(section));

        BusArrivalItemDto arrival = BusArrivalItemDto.builder()
                .routeNo("순환46")
                .arrivalEstimateTime("180")
                .restStopCount("2")
                .build();
        given(busService.getRealtimeArrivals("38391")).willReturn(List.of(arrival));
        given(busService.getHistory("38391", null)).willReturn(
                BusHistoryResponseDto.builder().averageIntervalSeconds(360).build()
        );

        AgentTool.ToolResult result = busAgentTool.execute(null, Map.of("stopName", "2번 출구 버스"));

        assertNotNull(result);
        assertNotNull(result.uiComponent());
        assertEquals("BUS", result.uiComponent().type());
        assertTrue(result.summary().contains("[인입런 - 인천대입구역 2번출구]"));
        assertTrue(result.summary().contains("순환46번: 약 3분 (2개 정류소 전)"));
        // fallback 문구가 아닌 정상 매칭이어야 함
        assertFalse(result.summary().contains("정류소를 찾지 못해"));
    }

    @Test
    @DisplayName("출구 번호가 포함된 경우 fallbackParams에서도 '인천대입구역 2번출구'로 정규화된다")
    void fallbackParamsNormalizesExitKeyword() {
        assertTrue(busAgentTool.supportsFallback("2번 출구 버스 언제 와?", Collections.emptyList()));
        Map<String, Object> params = busAgentTool.createFallbackParams("2번 출구 버스 언제 와?", Collections.emptyList());
        assertEquals("인천대입구역 2번출구", params.get("stopName"));
    }

    @Test
    @DisplayName("일치하는 정류소가 전혀 없는 경우 시간대 기본 정류소로 대체되며 정직한 대체 안내 문구가 포함된다")
    void executeWithUnknownStopIncludesHonestFallbackNotice() {
        BusStopAliasDto alias = BusStopAliasDto.builder()
                .bstopId("38391")
                .stopAlias("인천대입구역 2번출구")
                .build();

        BusRouteSectionResponseDto section = BusRouteSectionResponseDto.builder()
                .id(1L)
                .category("go-school")
                .tabName("인입런")
                .startBstopId("38391")
                .startBstopName("인천대입구역")
                .startBstopAlias("인천대입구역 2번출구")
                .routeNo("8")
                .build();

        given(busService.getStopAliases()).willReturn(List.of(alias));
        given(busService.getRouteSections(null)).willReturn(List.of(section));
        given(busService.getRealtimeArrivals(any())).willReturn(Collections.emptyList());

        AgentTool.ToolResult result = busAgentTool.execute(null, Map.of("stopName", "완전이상한정류장"));

        assertNotNull(result);
        assertTrue(result.summary().contains("요청하신 '완전이상한정류장' 정류소를 찾지 못해"));
    }
}
