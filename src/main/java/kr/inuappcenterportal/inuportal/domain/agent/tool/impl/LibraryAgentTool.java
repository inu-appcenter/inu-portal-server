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
        return new AgentToolDefinition("LIBRARY", "학산도서관 좌석/스터디룸 조회와 모바일 앱 위임 실행(배정·예약·연장·반납)을 처리합니다.",
                List.of("열람실·노트북실·라운지 실시간 잔여 좌석 조회", "특정 열람실 좌석 배정 신청 및 점유 시 빈자리 알림 제안", "스터디룸 목록과 이용 정보 조회",
                        "스터디룸 날짜/시간/동반자 예약 신청", "내 좌석 조회·연장·반납", "스터디룸 예약·취소·체크인"),
                List.of("도서관 자리 있어?", "자연과학열람실 30번 자리 예약해줘", "내일 205호 스터디룸 예약해줘", "내 좌석 연장해줘", "좌석 반납할게", "스터디룸 예약 취소해줘"),
                List.of("빈자리·취소표 감시는 ACTION_CAMPUS_WATCH", "모든 배정·예약 변경 작업은 모바일 앱에서 사용자 최종 확인 카드 동의 후 실행"),
                Map.ofEntries(
                        Map.entry("target", AgentToolParameter.string("수행 대상", false, "SEATS", "STUDY_ROOMS", "RESERVE_SEAT", "RESERVE_STUDY_ROOM", "MY_SEAT", "RENEW_SEAT", "RETURN_SEAT", "CANCEL_STUDY_ROOM", "CHECKIN_STUDY_ROOM")),
                        Map.entry("roomName", AgentToolParameter.string("열람실 또는 스터디룸 이름 (예: 자연과학열람실, 205호)", false)),
                        Map.entry("seatNo", AgentToolParameter.string("열람실 좌석 번호 (예: 30)", false)),
                        Map.entry("roomId", AgentToolParameter.integer("열람실 또는 스터디룸 ID", false)),
                        Map.entry("date", AgentToolParameter.string("이용 희망 날짜 (YYYY-MM-DD)", false)),
                        Map.entry("beginTime", AgentToolParameter.string("시작 시간 (HH:mm)", false)),
                        Map.entry("endTime", AgentToolParameter.string("종료 시간 (HH:mm)", false)),
                        Map.entry("purpose", AgentToolParameter.string("이용 목적 (예: 조별 과제, 스터디)", false)),
                        Map.entry("companionCnt", AgentToolParameter.integer("동반 이용자 수", false)),
                        Map.entry("companionPatrons", AgentToolParameter.string("동반 이용자 정보 (이름, 학번 등)", false)),
                        Map.entry("chargeId", AgentToolParameter.integer("예약 또는 좌석 이용 번호", false))
                ), false, false);
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        String target = "SEATS";
        String roomName = "";
        String seatNo = "";
        Integer roomId = null;

        if (params != null) {
            if (params.containsKey("target") && params.get("target") != null) {
                target = String.valueOf(params.get("target")).toUpperCase();
            }
            if (params.containsKey("roomName") && params.get("roomName") != null) {
                roomName = String.valueOf(params.get("roomName")).trim();
            }
            if (params.containsKey("seatNo") && params.get("seatNo") != null) {
                seatNo = String.valueOf(params.get("seatNo")).trim();
            }
            if (params.containsKey("roomId") && params.get("roomId") != null) {
                try {
                    roomId = Integer.parseInt(String.valueOf(params.get("roomId")));
                } catch (Exception ignored) {}
            }
        }

        log.info("[LibraryAgentTool] execute target: {}, roomName: {}, seatNo: {}, roomId: {}", target, roomName, seatNo, roomId);

        // 1. 열람실 특정 좌석 배정 신청 (RESERVE_SEAT)
        if ("RESERVE_SEAT".equalsIgnoreCase(target)) {
            Map<String, Object> seatData = new LinkedHashMap<>();
            seatData.put("roomName", roomName.isBlank() ? "열람실" : roomName);
            seatData.put("seatNo", seatNo);
            if (roomId != null) seatData.put("roomId", roomId);

            // 열람실 목록에서 roomId 매핑 시도
            if (roomId == null && !roomName.isBlank()) {
                Integer matchedRoomId = findReadingRoomIdByName(roomName);
                if (matchedRoomId != null) {
                    seatData.put("roomId", matchedRoomId);
                }
            }

            UiComponentDto ui = UiComponentDto.of(
                    "LIBRARY_SEAT_CONFIRM",
                    seatData,
                    "학산도서관 좌석 배정 확인",
                    "/services/library"
            );

            String summary = String.format("%s %s번 좌석 배정을 확인하실 수 있도록 준비했습니다. 현재 좌석 점유 상태를 확인하고 배정 또는 빈자리 알림을 설정하세요.",
                    seatData.get("roomName"), seatNo.isBlank() ? "" : seatNo);
            return new ToolResult(summary, ui, seatData);
        }

        // 2. 스터디룸 예약 신청 (RESERVE_STUDY_ROOM)
        if ("RESERVE_STUDY_ROOM".equalsIgnoreCase(target)) {
            Map<String, Object> studyData = new LinkedHashMap<>();
            studyData.put("roomName", roomName.isBlank() ? "스터디룸" : roomName);
            if (roomId != null) studyData.put("roomId", roomId);
            if (params != null) {
                if (params.get("date") != null) studyData.put("date", params.get("date"));
                if (params.get("beginTime") != null) studyData.put("beginTime", params.get("beginTime"));
                if (params.get("endTime") != null) studyData.put("endTime", params.get("endTime"));
                if (params.get("purpose") != null) studyData.put("purpose", params.get("purpose"));
                if (params.get("companionCnt") != null) studyData.put("companionCnt", params.get("companionCnt"));
                if (params.get("companionPatrons") != null) studyData.put("companionPatrons", params.get("companionPatrons"));
            }

            // 스터디룸 ID 매핑 시도
            if (roomId == null && !roomName.isBlank()) {
                Integer matchedStudyRoomId = findStudyRoomIdByName(roomName);
                if (matchedStudyRoomId != null) {
                    studyData.put("roomId", matchedStudyRoomId);
                }
            }

            UiComponentDto ui = UiComponentDto.of(
                    "LIBRARY_STUDY_ROOM_CONFIRM",
                    studyData,
                    "학산도서관 스터디룸 예약 확인",
                    "/services/library"
            );

            String summary = String.format("%s 스터디룸 예약 정보를 확인해 주세요. 신청하기를 누르시면 도서관 계정으로 즉시 예약이 진행됩니다.",
                    studyData.get("roomName"));
            return new ToolResult(summary, ui, studyData);
        }

        // 3. 기존 기타 클라이언트 액션 (MY_SEAT, RENEW_SEAT, RETURN_SEAT, CANCEL_STUDY_ROOM, CHECKIN_STUDY_ROOM)
        if (Set.of("MY_SEAT", "RENEW_SEAT", "RETURN_SEAT", "CANCEL_STUDY_ROOM", "CHECKIN_STUDY_ROOM").contains(target)) {
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("action", target);
            if (!roomName.isBlank()) action.put("roomName", roomName);
            if (params != null && params.get("chargeId") != null) action.put("chargeId", params.get("chargeId"));
            boolean mutation = !"MY_SEAT".equals(target);
            String summary = mutation
                    ? "모바일 앱에서 내용을 확인한 뒤 실행할 수 있도록 준비했습니다."
                    : "모바일 앱의 도서관 계정으로 현재 이용 중인 좌석을 확인할 수 있습니다.";
            return new ToolResult(summary, UiComponentDto.of("LIBRARY_CLIENT_ACTION", action), action);
        }

        // 4. 스터디룸 목록 조회
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
                    "학산도서관 스터디룸 목록",
                    "/services/library?tab=study"
            );

            StringBuilder sb = new StringBuilder("현재 예약 가능한 학산도서관 스터디룸 안내입니다:\n");
            sb.append("• 중앙관 2층: 205호~209호 (2~8인실, 전자칠판 및 모니터 구비)\n");
            sb.append("• 중앙관 3층: 305호~309호 (3~14인실 대형 그룹 스터디룸)\n");
            sb.append("• 이룸관 3층: 스터디룸 1호~6호 (2~8인실 집중 토의실)\n");
            sb.append("희망하시는 스터디룸과 시간, 동반자를 말씀해 주시면 바로 예약을 도와드립니다.");

            return new ToolResult(sb.toString(), ui, Map.of("rooms", studyRooms));
        }

        // 5. 실시간 열람실 좌석 조회 (lib.inu.ac.kr 공개 API 직접 호출)
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
                            "/services/library"
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
                "/services/library"
        );
        String fallbackSummary = "현재 학산도서관 실시간 좌석 정보를 직접 조회하는 중 일시적인 연결 오류가 발생했습니다. 앱 내 도서관 탭에서 좌석 현황을 확인하실 수 있습니다.";
        return new ToolResult(fallbackSummary, ui, Map.of("target", target));
    }

    private Integer findReadingRoomIdByName(String name) {
        if (name == null || name.isBlank()) return null;
        String clean = name.replaceAll("\\s+", "");
        if (clean.contains("제1") || clean.contains("1열람실")) return 1;
        if (clean.contains("제2") || clean.contains("2열람실")) return 2;
        if (clean.contains("자연과학")) return 3;
        if (clean.contains("집중")) return 4;
        if (clean.contains("노트북")) return 5;
        if (clean.contains("자유") || clean.contains("열린")) return 6;
        return null;
    }

    private Integer findStudyRoomIdByName(String name) {
        if (name == null || name.isBlank()) return null;
        if (name.contains("205")) return 9;
        if (name.contains("206")) return 10;
        if (name.contains("207")) return 11;
        if (name.contains("208")) return 12;
        if (name.contains("209")) return 13;
        if (name.contains("305")) return 14;
        if (name.contains("306")) return 15;
        if (name.contains("1호") || name.contains("스터디룸-1")) return 41;
        if (name.contains("2호") || name.contains("스터디룸-2")) return 42;
        if (name.contains("5호") || name.contains("스터디룸-5")) return 45;
        return null;
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
