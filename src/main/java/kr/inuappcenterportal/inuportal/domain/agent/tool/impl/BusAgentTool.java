package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusArrivalItemDto;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopAliasDto;
import kr.inuappcenterportal.inuportal.domain.bus.service.BusService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusAgentTool implements AgentTool {

    private final BusService busService;

    @Override
    public String getName() {
        return "BUS";
    }

    @Override
    public String getDescription() {
        return "셔틀버스, 시내버스, 버스 도착 시간, 정류장 관련 질문 (params: {\"stopName\": \"정문\"|\"공과대\"|\"자연대\"|\"송도역\" 등})";
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        try {
            String targetStopName = "정문";
            if (params != null && params.containsKey("stopName") && params.get("stopName") != null) {
                String candidate = String.valueOf(params.get("stopName")).trim();
                if (!candidate.isBlank()) {
                    targetStopName = candidate;
                }
            }

            List<BusStopAliasDto> aliases = busService.getStopAliases();
            String matchedBstopId = null;
            String resolvedStopName = targetStopName;

            for (BusStopAliasDto alias : aliases) {
                if (alias.getStopAlias().contains(targetStopName) || targetStopName.contains(alias.getStopAlias()) ||
                    (alias.getBstopName() != null && alias.getBstopName().contains(targetStopName))) {
                    matchedBstopId = alias.getBstopId();
                    resolvedStopName = alias.getStopAlias();
                    break;
                }
            }

            if (matchedBstopId == null && !aliases.isEmpty()) {
                matchedBstopId = aliases.get(0).getBstopId();
                resolvedStopName = aliases.get(0).getStopAlias();
            }

            List<BusArrivalItemDto> arrivals = matchedBstopId != null
                    ? busService.getRealtimeArrivals(matchedBstopId)
                    : Collections.emptyList();

            Map<String, Object> busData = new LinkedHashMap<>();
            busData.put("stopName", resolvedStopName);
            busData.put("bstopId", matchedBstopId);
            busData.put("arrivals", arrivals);

            UiComponentDto component = UiComponentDto.of("BUS", busData, "버스 실시간 운행정보", "/bus");

            StringBuilder summary = new StringBuilder();
            summary.append(String.format("[%s] 정류소 실시간 버스 도착 정보입니다.\n", resolvedStopName));
            if (arrivals.isEmpty()) {
                summary.append("현재 운행 대기 중이거나 도착 예정인 버스가 없습니다.");
            } else {
                for (int i = 0; i < Math.min(arrivals.size(), 3); i++) {
                    BusArrivalItemDto item = arrivals.get(i);
                    int arrivalMin = 0;
                    try {
                        if (item.getArrivalEstimateTime() != null) {
                            arrivalMin = Integer.parseInt(item.getArrivalEstimateTime()) / 60;
                        }
                    } catch (Exception ignored) {}
                    summary.append(String.format("• %s: 약 %d분 (%s개 정류소 전)\n", 
                            item.getRouteNo(), arrivalMin, item.getRestStopCount()));
                }
            }

            return new ToolResult(summary.toString().trim(), component, busData);
        } catch (Exception e) {
            log.error("버스 도구 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("버스 도착 정보를 가져오는 데 실패했습니다.", null, null);
        }
    }
}
