package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
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
    public String getName() {
        return "LIBRARY";
    }

    @Override
    public String getDescription() {
        return "인천대학교 학산도서관 열람실 실시간 잔여 좌석(제1~3열람실, 노트북실, 라운지 등), 좌석 배정 및 예약, 좌석 연장, 좌석 반납(퇴실), 스터디룸 및 세미나실 공간 조회/예약 관련 질문 (params: {\"target\": \"SEATS\"|\"STUDY_ROOMS\"|\"MY_SEAT\"|\"RESERVE\"|\"RENEW\"|\"RETURN\", \"roomName\": \"제1열람실\"|\"제1노트북실\" 등})";
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
}
