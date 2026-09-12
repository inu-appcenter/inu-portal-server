package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusArrivalItemDto;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusHistoryResponseDto;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusRouteSectionResponseDto;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopAliasDto;
import kr.inuappcenterportal.inuportal.domain.bus.service.BusService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
        return "셔틀버스, 시내버스 실시간 도착 시간, 과거 도착 이력/시간표, 배차 간격 통계 질문 (params: {\"stopName\": \"정문\"|\"공과대\"|\"자연대\"|\"인입\"|\"송도역\" 등, \"targetDate\": \"YYYY-MM-DD\"|\"YESTERDAY\"|\"LAST_WEEK\" (선택), \"mode\": \"REALTIME\"|\"HISTORY\" (선택)})";
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

            String mode = "REALTIME";
            if (params != null && params.containsKey("mode") && params.get("mode") != null) {
                String m = String.valueOf(params.get("mode")).trim().toUpperCase();
                if (m.contains("HIST") || m.contains("과거") || m.contains("통계")) {
                    mode = "HISTORY";
                }
            }

            String targetDateStr = null;
            if (params != null && params.containsKey("targetDate") && params.get("targetDate") != null) {
                String rawDate = String.valueOf(params.get("targetDate")).trim();
                if (rawDate.equalsIgnoreCase("YESTERDAY") || rawDate.contains("어제")) {
                    targetDateStr = LocalDate.now().minusDays(1).toString();
                    mode = "HISTORY";
                } else if (rawDate.equalsIgnoreCase("LAST_WEEK") || rawDate.contains("지난주")) {
                    targetDateStr = LocalDate.now().minusWeeks(1).toString();
                    mode = "HISTORY";
                } else if (rawDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    targetDateStr = rawDate;
                    mode = "HISTORY";
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

            Map<String, Object> busData = new LinkedHashMap<>();
            busData.put("stopName", resolvedStopName);
            busData.put("tabName", matchedTabName);
            busData.put("category", matchedCategory);
            busData.put("bstopId", matchedBstopId);
            busData.put("validRoutes", new ArrayList<>(validRouteNos));

            String redirectUrl = String.format("/bus/info?type=%s&category=%s",
                    URLEncoder.encode(matchedCategory, StandardCharsets.UTF_8),
                    URLEncoder.encode(matchedTabName, StandardCharsets.UTF_8));

            StringBuilder summary = new StringBuilder();

            if ("HISTORY".equalsIgnoreCase(mode)) {
                // 과거 도착 이력 및 평균 배차 통계 조회
                BusHistoryResponseDto historyDto = (matchedBstopId != null)
                        ? busService.getHistory(matchedBstopId, targetDateStr)
                        : null;

                int avgMin = 0;
                List<Map<String, Object>> formattedRecords = new ArrayList<>();

                if (historyDto != null) {
                    if (historyDto.getAverageIntervalSeconds() != null && historyDto.getAverageIntervalSeconds() > 0) {
                        avgMin = historyDto.getAverageIntervalSeconds() / 60;
                    }

                    if (historyDto.getHistoryRecords() != null) {
                        List<BusHistoryResponseDto.HistoryRecord> filtered = historyDto.getHistoryRecords().stream()
                                .filter(r -> validRouteNos.isEmpty() || validRouteNos.contains(r.getRouteNo()))
                                .collect(Collectors.toList());

                        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
                        for (BusHistoryResponseDto.HistoryRecord r : filtered) {
                            Map<String, Object> rec = new LinkedHashMap<>();
                            rec.put("routeNo", r.getRouteNo());
                            rec.put("busNumPlate", r.getBusNumPlate());
                            rec.put("time", (r.getArrivalTime() != null) ? r.getArrivalTime().format(timeFmt) : "");
                            formattedRecords.add(rec);
                        }
                    }
                }

                busData.put("mode", "HISTORY");
                busData.put("targetDate", historyDto != null ? historyDto.getTargetDate() : targetDateStr);
                busData.put("dayOfWeek", historyDto != null ? historyDto.getDayOfWeek() : "");
                busData.put("averageIntervalMinutes", avgMin);
                busData.put("historyRecords", formattedRecords);

                summary.append(String.format("[%s - %s] 버스 과거 도착 이력 및 배차 통계입니다 (기준일: %s).\n\n",
                        matchedTabName, resolvedStopName, busData.get("targetDate")));

                if (avgMin > 0) {
                    summary.append(String.format("• 동일 요일 최근 4주 평균 배차 간격: 약 %d분\n", avgMin));
                }

                if (formattedRecords.isEmpty()) {
                    summary.append(String.format("해당 일자에는 인입런 모니터링 버스(%s)의 정차 이력이 없습니다.",
                            String.join(", ", validRouteNos)));
                } else {
                    summary.append(String.format("• 당일 실제 정차 기록 (%d건 중 최근 %d건):\n",
                            formattedRecords.size(), Math.min(formattedRecords.size(), 4)));
                    int startIdx = Math.max(0, formattedRecords.size() - 4);
                    for (int i = formattedRecords.size() - 1; i >= startIdx; i--) {
                        Map<String, Object> rec = formattedRecords.get(i);
                        summary.append(String.format("  - %s번 (%s): %s 정차\n",
                                rec.get("routeNo"), rec.get("busNumPlate"), rec.get("time")));
                    }
                }

                UiComponentDto component = UiComponentDto.of("BUS", busData,
                        String.format("[%s] 과거 버스 시간표 보기", matchedTabName), redirectUrl);
                return new ToolResult(summary.toString().trim(), component, busData);

            } else {
                // 실시간 도착 정보 조회
                List<BusArrivalItemDto> rawArrivals = matchedBstopId != null
                        ? busService.getRealtimeArrivals(matchedBstopId)
                        : Collections.emptyList();

                List<BusArrivalItemDto> arrivals = rawArrivals.stream()
                        .filter(a -> validRouteNos.contains(a.getRouteNo()))
                        .collect(Collectors.toList());

                // 평균 배차 정보도 보조로 조회
                int avgMin = 0;
                try {
                    BusHistoryResponseDto hist = (matchedBstopId != null) ? busService.getHistory(matchedBstopId, null) : null;
                    if (hist != null && hist.getAverageIntervalSeconds() != null && hist.getAverageIntervalSeconds() > 0) {
                        avgMin = hist.getAverageIntervalSeconds() / 60;
                    }
                } catch (Exception ignored) {}

                busData.put("mode", "REALTIME");
                busData.put("arrivals", arrivals);
                busData.put("averageIntervalMinutes", avgMin);

                summary.append(String.format("[%s - %s] 실시간 인입런 버스 도착 정보입니다.\n", matchedTabName, resolvedStopName));
                if (avgMin > 0) {
                    summary.append(String.format("• 동요일 평균 배차 간격: 약 %d분\n", avgMin));
                }

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

                UiComponentDto component = UiComponentDto.of("BUS", busData,
                        String.format("[%s] 인입런 도착 정보 보기", matchedTabName), redirectUrl);
                return new ToolResult(summary.toString().trim(), component, busData);
            }
        } catch (Exception e) {
            log.error("버스 도구 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("버스 도착 정보를 가져오는 데 실패했습니다.", null, null);
        }
    }

    @Override
    public boolean supportsFallback(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        return lower.contains("버스") || lower.contains("셔틀") || lower.contains("정류장") || lower.contains("몇 분");
    }

    @Override
    public Map<String, Object> createFallbackParams(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        return Map.of("stopName", "정문");
    }
}
