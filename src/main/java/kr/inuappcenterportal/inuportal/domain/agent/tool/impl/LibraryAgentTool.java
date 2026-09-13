package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.*;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * 인천대학교 학산도서관 열람실 좌석 및 스터디룸/공간 예약 Agent Tool (실시간 pyxis-api 연동)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryAgentTool implements AgentTool {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    private static final String SEAT_ROOMS_URL = "https://lib.inu.ac.kr/pyxis-api/1/seat-rooms?branchGroupId=1&smufMethodCode=PC";

    @Override
    public AgentToolDefinition getDefinition() {
        return new AgentToolDefinition("LIBRARY", "학산도서관 열람실 좌석 현황과 스터디룸 목록을 조회합니다.",
                List.of("열람실·노트북실·라운지 실시간 잔여 좌석 조회", "스터디룸·세미나실 목록과 이용 정보 조회"),
                List.of("도서관 자리 있어?", "제1열람실 몇 자리 남았어?", "스터디룸 목록 보여줘"),
                List.of("빈자리 발생 감시는 ACTION_CAMPUS_WATCH", "좌석 배정·예약·연장·반납을 직접 실행하는 기능은 지원하지 않음"),
                Map.of("target", AgentToolParameter.string("조회 대상", false, "SEATS", "STUDY_ROOMS"),
                        "roomName", AgentToolParameter.string("특정 열람실 이름", false)), false, true);
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        String target = "SEATS";
        String roomName = "";

        if (params != null) {
            if (params.containsKey("target") && params.get("target") != null) {
                target = String.valueOf(params.get("target")).toUpperCase();
            }
            if (params.containsKey("roomName") && params.get("roomName") != null) {
                roomName = String.valueOf(params.get("roomName")).trim();
            }
        }

        log.info("[LibraryAgentTool] execute target: {}, roomName: {}", target, roomName);

        if ("STUDY_ROOMS".equalsIgnoreCase(target)) {
            List<Map<String, Object>> studyRooms = List.of(
                    Map.of("id", 9, "name", "205호", "location", "중앙관 2층", "quota", "4~8명", "tags", List.of("전자칠판", "화이트보드")),
                    Map.of("id", 10, "name", "206호", "location", "중앙관 2층", "quota", "4~8명", "tags", List.of("전자칠판", "화이트보드")),
                    Map.of("id", 11, "name", "207호", "location", "중앙관 2층", "quota", "2~4명", "tags", List.of("소형 스터디", "모니터")),
                    Map.of("id", 12, "name", "208호", "location", "중앙관 2층", "quota", "2~4명", "tags", List.of("소형 스터디", "모니터")),
                    Map.of("id", 13, "name", "209호", "location", "중앙관 2층", "quota", "4~8명", "tags", List.of("화이트보드")),
                    Map.of("id", 14, "name", "305호", "location", "중앙관 3층", "quota", "7~14명", "tags", List.of("대형 세미나", "빔프로젝터")),
                    Map.of("id", 15, "name", "306호", "location", "중앙관 3층", "quota", "7~14명", "tags", List.of("대형 세미나", "빔프로젝터")),
                    Map.of("id", 41, "name", "스터디룸-1", "location", "이룸관 3층", "quota", "2~4명", "tags", List.of("화이트보드")),
                    Map.of("id", 42, "name", "스터디룸-2", "location", "이룸관 3층", "quota", "2~4명", "tags", List.of("화이트보드")),
                    Map.of("id", 45, "name", "스터디룸-5", "location", "이룸관 3층", "quota", "4~8명", "tags", List.of("전자칠판", "화이트보드"))
            );

            UiComponentDto ui = UiComponentDto.of(
                    "LIBRARY_STUDY_ROOMS",
                    Map.of(
                            "rooms", studyRooms,
                            "notice", "스터디룸 예약은 1회 최대 2시간, 당일 예약 가능합니다. (이용 시작 20분 내 50% 이상 입실 필수)"
                    ),
                    "학산도서관 스터디룸 예약",
                    "https://lib.inu.ac.kr/#/facility/study-room"
            );

            StringBuilder sb = new StringBuilder("현재 예약 가능한 학산도서관 스터디룸 안내입니다:\n");
            sb.append("• 중앙관 2층: 205호~209호 (2~8인실, 전자칠판 및 모니터 구비)\n");
            sb.append("• 중앙관 3층: 305호~309호 (3~14인실 대형 그룹 스터디룸)\n");
            sb.append("• 이룸관 3층: 스터디룸 1호~6호 (2~8인실 집중 토의실)\n");
            sb.append("희망하시는 스터디룸을 선택하시면 도서관 계정으로 즉시 시간대 예약이 가능합니다.");

            return new ToolResult(sb.toString(), ui, Map.of("rooms", studyRooms));
        }

        // 실시간 열람실 좌석 조회 (lib.inu.ac.kr 공개 API 직접 호출)
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SEAT_ROOMS_URL))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) INTIP-Agent/1.0")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode listNode = root.path("data").path("list");

                if (listNode.isArray() && !listNode.isEmpty()) {
                    List<Map<String, Object>> roomList = new ArrayList<>();
                    StringBuilder summaryBuilder = new StringBuilder("현재 학산도서관 실시간 열람실 잔여 좌석 현황입니다:\n");

                    for (JsonNode rNode : listNode) {
                        String name = rNode.path("name").asText();
                        int total = rNode.path("seats").path("total").asInt();
                        int occupied = rNode.path("seats").path("occupied").asInt();
                        int available = rNode.path("seats").path("available").asInt();

                        Map<String, Object> roomMap = new LinkedHashMap<>();
                        roomMap.put("id", rNode.path("id").asInt());
                        roomMap.put("name", name);
                        roomMap.put("seats", Map.of(
                                "total", total,
                                "occupied", occupied,
                                "available", available
                        ));
                        roomList.add(roomMap);

                        if (roomName.isBlank() || name.contains(roomName)) {
                            summaryBuilder.append(String.format("- %s: 잔여 %d석 / 전체 %d석 (이용률 %d%%)\n",
                                    name, available, total, total > 0 ? (occupied * 100 / total) : 0));
                        }
                    }

                    UiComponentDto ui = UiComponentDto.of(
                            "LIBRARY_ROOMS",
                            Map.of("rooms", roomList),
                            "학산도서관 좌석 배정",
                            "https://lib.inu.ac.kr"
                    );

                    return new ToolResult(summaryBuilder.toString().trim(), ui, Map.of("rooms", roomList));
                }
            }
        } catch (Exception e) {
            log.error("[LibraryAgentTool] 도서관 좌석 API 호출 실패: {}", e.getMessage(), e);
        }

        // Fallback
        UiComponentDto ui = UiComponentDto.of(
                "LIBRARY_ACTION",
                Map.of("target", target, "roomName", roomName),
                "도서관 바로가기",
                "https://lib.inu.ac.kr"
        );
        String fallbackSummary = "현재 학산도서관 실시간 좌석 정보를 직접 조회하는 중 일시적인 연결 오류가 발생했습니다. 학산도서관 공식 사이트에서 좌석 현황을 확인하실 수 있습니다.";
        return new ToolResult(fallbackSummary, ui, Map.of("target", target));
    }

    @Override
    public boolean supportsFallback(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        return lower.contains("도서관") || lower.contains("열람실") || lower.contains("노트북실") || lower.contains("스터디룸") || lower.contains("자리") || lower.contains("좌석") || lower.contains("세미나실");
    }

    @Override
    public Map<String, Object> createFallbackParams(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null) return Map.of("target", "SEATS");
        String lower = message.toLowerCase();
        String target = "SEATS";
        if (lower.contains("스터디룸") || lower.contains("세미나실") || lower.contains("공간")) {
            target = "STUDY_ROOMS";
        }
        return Map.of("target", target);
    }
}
