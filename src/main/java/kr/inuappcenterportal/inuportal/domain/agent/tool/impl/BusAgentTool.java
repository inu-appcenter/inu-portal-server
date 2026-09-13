package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.*;
import kr.inuappcenterportal.inuportal.domain.agent.util.AgentFuzzyMatcher;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusAgentTool implements AgentTool {

    private final BusService busService;

    @Override
    public AgentToolDefinition getDefinition() {
        return new AgentToolDefinition("BUS", "교내 셔틀버스와 주변 시내버스 도착 정보를 조회합니다.",
                List.of("정류장별 실시간 도착 조회", "과거 날짜 도착 이력 조회", "배차 간격 통계 조회"),
                List.of("2번 출구 버스 언제 와?", "정문 버스 언제 와?", "인입 1번출구 버스", "송도역 셔틀 알려줘", "어제 공대 버스 배차 어땠어?"),
                List.of("특정 시각에 버스 정보를 알려달라는 예약 요청은 ACTION_MANAGE_REMINDER"),
                Map.of("stopName", AgentToolParameter.string("조회할 정류소 명칭 (예: 인천대입구역 1번출구, 인천대입구역 2번출구, 인천대입구역.롯데몰, 지식정보단지역 3번출구, 인천대 정문, 인천대 공과대학, 자연과학대학, 기숙사 등). 사용자가 '2번 출구'를 질의하면 지하철역 출구인 '인천대입구역 2번출구'로 지정하세요. 미지정 시 현재 시간대 기본 정류소.", false),
                        "targetDate", AgentToolParameter.string("YYYY-MM-DD, YESTERDAY 또는 LAST_WEEK", false),
                        "mode", AgentToolParameter.string("조회 방식", false, "REALTIME", "HISTORY")), false, true);
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        try {
            String rawStopName = "";
            boolean hasExplicitStopQuery = false;
            if (params != null && params.containsKey("stopName") && params.get("stopName") != null) {
                String candidate = String.valueOf(params.get("stopName")).trim();
                if (!candidate.isBlank()) {
                    rawStopName = candidate;
                    hasExplicitStopQuery = true;
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

            // 1. 질의어 기반 정류장/별칭/구간 탐색 (AgentFuzzyMatcher를 통한 자율 유사도 매칭)
            List<BusRouteSectionResponseDto> matchedSections = new ArrayList<>();
            String matchedBstopId = null;
            String resolvedStopName = null;
            boolean fallbackToDefaultUsed = false;

            if (hasExplicitStopQuery) {
                // 1-1. BusStopAlias(공식 명칭, 축약 별칭, 메모)에서 최적 일치 정류소 탐색
                Optional<AgentFuzzyMatcher.MatchResult<BusStopAliasDto>> aliasMatch = AgentFuzzyMatcher.findBestMatch(
                        rawStopName,
                        aliases,
                        a -> List.of(
                                a.getBstopName() != null ? a.getBstopName() : "",
                                a.getStopAlias() != null ? a.getStopAlias() : "",
                                a.getMemo() != null ? a.getMemo() : ""
                        ),
                        0.4
                );

                if (aliasMatch.isPresent()) {
                    BusStopAliasDto bestAlias = aliasMatch.get().item();
                    matchedBstopId = bestAlias.getBstopId();
                    resolvedStopName = (bestAlias.getBstopName() != null && !bestAlias.getBstopName().isBlank())
                            ? bestAlias.getBstopName()
                            : bestAlias.getStopAlias();
                }

                // 1-2. 정류장 ID가 특정되었으면 해당 ID의 노선 섹션들 선택
                if (matchedBstopId != null) {
                    for (BusRouteSectionResponseDto sec : allSections) {
                        if (matchedBstopId.equals(sec.getStartBstopId())) {
                            matchedSections.add(sec);
                        }
                    }
                }

                // 1-3. BusStopAlias 매칭 실패 시, 전체 노선 섹션(섹션명, 탭명, 기점 정류장명/별칭)에서 직접 유사도 매칭
                if (matchedSections.isEmpty()) {
                    Optional<AgentFuzzyMatcher.MatchResult<BusRouteSectionResponseDto>> sectionMatch = AgentFuzzyMatcher.findBestMatch(
                            rawStopName,
                            allSections,
                            s -> List.of(
                                    s.getTabName() != null ? s.getTabName() : "",
                                    s.getSectionName() != null ? s.getSectionName() : "",
                                    s.getStartBstopName() != null ? s.getStartBstopName() : "",
                                    s.getStartBstopAlias() != null ? s.getStartBstopAlias() : ""
                            ),
                            0.35
                    );

                    if (sectionMatch.isPresent()) {
                        BusRouteSectionResponseDto bestSec = sectionMatch.get().item();
                        String bestBstopId = bestSec.getStartBstopId();
                        for (BusRouteSectionResponseDto sec : allSections) {
                            if (bestBstopId != null && bestBstopId.equals(sec.getStartBstopId())) {
                                matchedSections.add(sec);
                            }
                        }
                        if (matchedSections.isEmpty()) {
                            matchedSections.add(bestSec);
                        }
                        matchedBstopId = bestBstopId;
                        resolvedStopName = bestSec.getStartBstopAlias() != null
                                ? bestSec.getStartBstopAlias()
                                : bestSec.getStartBstopName();
                    }
                }
            }

            // 1-4. 사용자가 정류장을 미지정했거나 매칭되지 않은 경우 시간대 기준 기본값 적용
            if (matchedSections.isEmpty() && !allSections.isEmpty()) {
                if (hasExplicitStopQuery) {
                    fallbackToDefaultUsed = true;
                }
                boolean isMorning = LocalTime.now().isBefore(LocalTime.of(14, 0));
                String defaultTab = isMorning ? "인입런" : "인천대 정문";
                matchedSections = allSections.stream()
                        .filter(s -> defaultTab.equals(s.getTabName()))
                        .collect(Collectors.toList());
                if (matchedSections.isEmpty()) {
                    matchedSections = allSections.subList(0, Math.min(allSections.size(), 3));
                }
            }

            if (matchedSections.isEmpty()) {
                return new ToolResult("조회 가능한 버스 정류장 정보를 찾지 못했습니다.", null, null);
            }

            BusRouteSectionResponseDto representativeSection = matchedSections.get(0);
            String matchedCategory = representativeSection.getCategory() != null ? representativeSection.getCategory() : "go-school";
            String matchedTabName = representativeSection.getTabName() != null ? representativeSection.getTabName() : "";
            if (matchedBstopId == null) {
                matchedBstopId = representativeSection.getStartBstopId();
            }
            if (resolvedStopName == null) {
                resolvedStopName = representativeSection.getStartBstopAlias() != null
                        ? representativeSection.getStartBstopAlias()
                        : representativeSection.getStartBstopName();
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

            if (fallbackToDefaultUsed) {
                summary.append(String.format("요청하신 '%s' 정류소를 찾지 못해, 현재 시간대 기본 정류소인 [%s - %s] 정보를 대신 안내해 드립니다.\n\n",
                        rawStopName, matchedTabName, resolvedStopName));
            }

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
                    summary.append(String.format("해당 일자에는 모니터링 버스(%s)의 정차 이력이 없습니다.",
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

                summary.append(String.format("[%s - %s] 실시간 버스 도착 정보입니다.\n", matchedTabName, resolvedStopName));
                if (avgMin > 0) {
                    summary.append(String.format("• 동요일 평균 배차 간격: 약 %d분\n", avgMin));
                }

                if (arrivals.isEmpty()) {
                    summary.append(String.format("현재 운행 대기 중이거나 도착 예정인 버스(%s)가 없습니다.", String.join(", ", validRouteNos)));
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
                        String.format("[%s] 버스 도착 정보 보기", matchedTabName), redirectUrl);
                return new ToolResult(summary.toString().trim(), component, busData);
            }
        } catch (Exception e) {
            log.error("버스 도구 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("버스 도착 정보를 가져오는 데 실패했습니다.", null, null);
        }
    }

    @Override
    public boolean supportsFallback(String message, List<ChatMessageDto> history) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        return lower.contains("버스") || lower.contains("셔틀") || lower.contains("정류장")
                || lower.contains("몇 분") || lower.contains("출구");
    }

    @Override
    public Map<String, Object> createFallbackParams(String message, List<ChatMessageDto> history) {
        String stop = "";
        try {
            List<BusStopAliasDto> aliases = busService.getStopAliases();
            Optional<AgentFuzzyMatcher.MatchResult<BusStopAliasDto>> match = AgentFuzzyMatcher.findBestMatch(
                    message,
                    aliases,
                    a -> List.of(
                            a.getBstopName() != null ? a.getBstopName() : "",
                            a.getStopAlias() != null ? a.getStopAlias() : "",
                            a.getMemo() != null ? a.getMemo() : ""
                    ),
                    0.35
            );
            if (match.isPresent()) {
                BusStopAliasDto alias = match.get().item();
                stop = (alias.getBstopName() != null && !alias.getBstopName().isBlank())
                        ? alias.getBstopName()
                        : alias.getStopAlias();
            }
        } catch (Exception ignored) {}

        if (stop.isBlank()) {
            boolean isMorning = LocalTime.now().isBefore(LocalTime.of(14, 0));
            stop = isMorning ? "인입" : "정문";
        }
        return Map.of("stopName", stop);
    }
}
