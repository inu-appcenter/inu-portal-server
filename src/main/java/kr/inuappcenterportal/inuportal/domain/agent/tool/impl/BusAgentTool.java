package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusArrivalItemDto;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusRouteSectionResponseDto;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopAliasDto;
import kr.inuappcenterportal.inuportal.domain.bus.service.BusService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

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
        return "셔틀버스, 시내버스, 버스 도착 시간, 정류장 관련 질문 (params: {\"stopName\": \"정문\"|\"공과대\"|\"자연대\"|\"인입\"|\"송도역\" 등})";
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

            List<BusRouteSectionResponseDto> allSections = busService.getRouteSections(null);
            List<BusStopAliasDto> aliases = busService.getStopAliases();

            // 1. 인입런 정규 구간(BusRouteSection)에서 질의 정류소와 일치하는 섹션 탐색
            List<BusRouteSectionResponseDto> matchedSections = new ArrayList<>();
            for (BusRouteSectionResponseDto sec : allSections) {
                boolean matches = (sec.getTabName() != null && sec.getTabName().contains(targetStopName)) ||
                        (sec.getStartBstopName() != null && sec.getStartBstopName().contains(targetStopName)) ||
                        (sec.getStartBstopAlias() != null && (sec.getStartBstopAlias().contains(targetStopName) || targetStopName.contains(sec.getStartBstopAlias()))) ||
                        (sec.getSectionName() != null && sec.getSectionName().contains(targetStopName));
                if (matches) {
                    matchedSections.add(sec);
                }
            }

            // 매칭된 섹션이 없으면 시간대 기준 기본값(14시 이전 등교: 인입런, 이후 하교: 정문) 설정
            if (matchedSections.isEmpty() && !allSections.isEmpty()) {
                boolean isMorning = LocalTime.now().isBefore(LocalTime.of(14, 0));
                String defaultTab = isMorning ? "인입런" : "인천대 정문";
                matchedSections = allSections.stream()
                        .filter(s -> defaultTab.equals(s.getTabName()))
                        .collect(Collectors.toList());
                if (matchedSections.isEmpty()) {
                    matchedSections = allSections.subList(0, Math.min(allSections.size(), 3));
                }
            }

            BusRouteSectionResponseDto representativeSection = matchedSections.get(0);
            String matchedCategory = representativeSection.getCategory() != null ? representativeSection.getCategory() : "go-school";
            String matchedTabName = representativeSection.getTabName() != null ? representativeSection.getTabName() : targetStopName;
            String matchedBstopId = representativeSection.getStartBstopId();
            String resolvedStopName = representativeSection.getStartBstopAlias() != null
                    ? representativeSection.getStartBstopAlias()
                    : representativeSection.getStartBstopName();

            if (matchedBstopId == null) {
                for (BusStopAliasDto alias : aliases) {
                    if (alias.getStopAlias().contains(targetStopName) || targetStopName.contains(alias.getStopAlias())) {
                        matchedBstopId = alias.getBstopId();
                        resolvedStopName = alias.getStopAlias();
                        break;
                    }
                }
            }

            // 2. 인입런에서 실제로 관리/표출 중인 유효 버스 노선 번호 화이트리스트 구성
            Set<String> validRouteNos = matchedSections.stream()
                    .map(BusRouteSectionResponseDto::getRouteNo)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // 3. 실시간 도착 정보 조회 후 인입런 노선만 정밀 필터링
            List<BusArrivalItemDto> rawArrivals = matchedBstopId != null
                    ? busService.getRealtimeArrivals(matchedBstopId)
                    : Collections.emptyList();

            List<BusArrivalItemDto> arrivals = rawArrivals.stream()
                    .filter(a -> validRouteNos.contains(a.getRouteNo()))
                    .collect(Collectors.toList());

            Map<String, Object> busData = new LinkedHashMap<>();
            busData.put("stopName", resolvedStopName);
            busData.put("tabName", matchedTabName);
            busData.put("category", matchedCategory);
            busData.put("bstopId", matchedBstopId);
            busData.put("arrivals", arrivals);

            String redirectUrl = String.format("/bus/info?type=%s&category=%s",
                    URLEncoder.encode(matchedCategory, StandardCharsets.UTF_8),
                    URLEncoder.encode(matchedTabName, StandardCharsets.UTF_8));
            UiComponentDto component = UiComponentDto.of("BUS", busData, String.format("[%s] 인입런 도착 정보 보기", matchedTabName), redirectUrl);

            StringBuilder summary = new StringBuilder();
            summary.append(String.format("[%s - %s] 실시간 인입런 버스 도착 정보입니다.\n", matchedTabName, resolvedStopName));
            if (arrivals.isEmpty()) {
                summary.append(String.format("현재 운행 대기 중이거나 도착 예정인 인입런 버스(%s)가 없습니다.", String.join(", ", validRouteNos)));
            } else {
                for (int i = 0; i < Math.min(arrivals.size(), 3); i++) {
                    BusArrivalItemDto item = arrivals.get(i);
                    int arrivalMin = 0;
                    try {
                        if (item.getArrivalEstimateTime() != null) {
                            arrivalMin = Integer.parseInt(item.getArrivalEstimateTime()) / 60;
                        }
                    } catch (Exception ignored) {}
                    summary.append(String.format("• %s번: 약 %d분 (%s개 정류소 전)\n", 
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
