package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.CampusWatchCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.CampusWatchJobDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.enums.CampusWatchDomain;
import kr.inuappcenterportal.inuportal.domain.agent.service.CampusWatchService;
import kr.inuappcenterportal.inuportal.domain.agent.tool.*;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampusWatchAgentTool implements AgentTool {

    private final CampusWatchService campusWatchService;

    @Override
    public AgentToolDefinition getDefinition() {
        return new AgentToolDefinition("ACTION_CAMPUS_WATCH", "도서관 빈자리·스터디룸 취소표 감시를 등록·조회·취소합니다.",
                List.of("열람실·노트북실·힐링존 빈자리 감시 등록", "특정 좌석 및 스터디룸 취소표의 기기 내 감시", "감시 목록 조회", "감시 작업 ID로 취소"),
                List.of("제1열람실 자리 나면 알려줘", "205호 취소표 나오면 알려줘", "내 빈자리 감시 목록 보여줘"),
                List.of("현재 잔여 좌석만 조회할 때는 LIBRARY"),
                Map.of("action", AgentToolParameter.string("수행 작업", true, "WATCH", "LIST", "CANCEL"),
                        "domain", AgentToolParameter.string("감시 영역", false, "LIBRARY_SEAT", "STUDY_ROOM"),
                        "targetName", AgentToolParameter.string("열람실·노트북실·힐링존 이름", false),
                        "roomId", AgentToolParameter.integer("도서관 방 식별 번호", false),
                        "seatNo", AgentToolParameter.string("특정 좌석 번호", false),
                        "hopeDate", AgentToolParameter.string("스터디룸 희망 날짜(YYYY-MM-DD)", false),
                        "targetHour", AgentToolParameter.integer("스터디룸 희망 시작 시각(0~23)", false),
                        "durationMinutes", AgentToolParameter.integer("감시 지속 시간, 최대 180분", false),
                        "jobId", AgentToolParameter.integer("취소할 감시 작업 ID", false)), true, false);
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        if (member == null) {
            return new ToolResult("빈자리 알림을 설정하려면 로그인이 필요합니다.",
                    UiComponentDto.of("AUTH_REQUIRED", Map.of(), "로그인하기", "/login"), null);
        }

        String action = params != null && params.get("action") != null
                ? String.valueOf(params.get("action")).toUpperCase().trim()
                : "WATCH";

        try {
            if ("LIST".equals(action)) {
                List<CampusWatchJobDto> jobs = campusWatchService.getMyWatchJobs(member);
                UiComponentDto ui = UiComponentDto.of(
                        "CAMPUS_WATCH_LIST",
                        Map.of("jobs", jobs),
                        "감시 목록 관리",
                        "/mypage/notification/smart-watch"
                );
                String summary = jobs.isEmpty()
                        ? "현재 실행 중인 실시간 빈자리 감시 작업이 없습니다."
                        : String.format("현재 총 %d건의 실시간 감시 작업이 진행 중입니다.", jobs.size());
                return new ToolResult(summary, ui, Map.of("jobs", jobs));
            }

            if ("CANCEL".equals(action)) {
                if (params == null || params.get("jobId") == null) {
                    return new ToolResult("취소할 감시 작업 ID가 필요합니다. 먼저 감시 목록을 조회해 주세요.", null, null);
                }
                Long jobId = Long.valueOf(String.valueOf(params.get("jobId")));
                campusWatchService.cancelWatchJob(member, jobId);
                return new ToolResult("요청하신 빈자리 감시를 취소했습니다.", null, Map.of("cancelledJobId", jobId));
            }

            // 기본: 감시 등록
            String targetName = (params != null && params.get("targetName") != null)
                    ? String.valueOf(params.get("targetName")).trim()
                    : "제1열람실";

            String seatNo = (params != null && params.get("seatNo") != null)
                    ? String.valueOf(params.get("seatNo")).trim()
                    : null;

            String domainStr = (params != null && params.get("domain") != null)
                    ? String.valueOf(params.get("domain")).toUpperCase().trim()
                    : (targetName.contains("호") || targetName.contains("스터디") ? "STUDY_ROOM" : "LIBRARY_SEAT");

            CampusWatchDomain domain = "STUDY_ROOM".equals(domainStr)
                    ? CampusWatchDomain.STUDY_ROOM
                    : CampusWatchDomain.LIBRARY_SEAT;

            // 스터디룸 취소표와 특정 좌석 감시는 로그인 토큰을 보관하는 앱에서만 정확히 폴링할 수 있다.
            if (domain == CampusWatchDomain.STUDY_ROOM || (seatNo != null && !seatNo.isBlank())) {
                Map<String, Object> localAction = new java.util.LinkedHashMap<>();
                localAction.put("watchType", domain == CampusWatchDomain.STUDY_ROOM ? "STUDY_ROOM_SNIPER" : "SPECIFIC_SEAT_SNIPER");
                localAction.put("targetName", targetName);
                if (params != null && params.get("roomId") != null) localAction.put("roomId", params.get("roomId"));
                localAction.put("seatNo", seatNo);
                if (params != null && params.get("hopeDate") != null) localAction.put("hopeDate", params.get("hopeDate"));
                if (params != null && params.get("targetHour") != null) localAction.put("targetHour", params.get("targetHour"));
                localAction.put("durationMinutes", params != null && params.get("durationMinutes") != null ? params.get("durationMinutes") : 90);
                return new ToolResult("모바일 앱에서 빈자리 감시 조건을 확인한 뒤 기기 알림으로 등록할 수 있습니다.",
                        UiComponentDto.of("LOCAL_WATCH_ACTION", localAction), localAction);
            }

            int duration = 90;
            if (params != null && params.get("durationMinutes") != null) {
                try {
                    duration = Integer.parseInt(String.valueOf(params.get("durationMinutes")));
                } catch (Exception ignored) {}
            }

            String fullTargetName = (seatNo != null && !seatNo.isBlank())
                    ? String.format("%s %s번 좌석", targetName, seatNo)
                    : targetName;

            CampusWatchCreateRequestDto req = new CampusWatchCreateRequestDto(
                    domain,
                    fullTargetName,
                    fullTargetName,
                    duration
            );

            CampusWatchJobDto job = campusWatchService.registerWatch(member, req);

            UiComponentDto ui = UiComponentDto.of(
                    "CAMPUS_WATCH_RESULT",
                    Map.of(
                            "job", job,
                            "targetName", fullTargetName,
                            "domain", domain.name(),
                            "remainingMinutes", job.remainingMinutes()
                    ),
                    "감시 현황 확인하기",
                    "/mypage/notification/smart-watch"
            );

            String summary = String.format(
                    "학산도서관 [%s] 실시간 감시를 시작했습니다! 🎯\n최대 %d분 동안 안전하게 감시하며, 이용이 가능해지는 즉시 푸시 알림으로 알려드릴게요.",
                    fullTargetName, duration
            );

            return new ToolResult(summary, ui, Map.of("job", job));

        } catch (Exception e) {
            log.error("[CampusWatchAgentTool] 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("빈자리 감시 처리 중 오류가 발생했습니다: " + e.getMessage(), null, null);
        }
    }

    @Override
    public boolean supportsFallback(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        boolean isWatchTarget = lower.contains("빈자리") || ((lower.contains("힐링존") || lower.contains("수면실") || lower.contains("열람실")) && (lower.contains("자리 나면") || lower.contains("자리 생기면") || lower.contains("알려줘")));
        boolean isWatchList = lower.contains("감시") && (lower.contains("목록") || lower.contains("조회") || lower.contains("현황"));
        boolean isCancel = (lower.contains("감시") || lower.contains("스나이퍼")) && (lower.contains("취소") || lower.contains("삭제") || lower.contains("그만"));
        return isWatchTarget || isWatchList || isCancel;
    }

    @Override
    public Map<String, Object> createFallbackParams(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null) return Map.of("action", "LIST");
        String lower = message.toLowerCase();
        if (lower.contains("감시") && (lower.contains("목록") || lower.contains("조회") || lower.contains("현황"))) {
            return Map.of("action", "LIST");
        }
        if ((lower.contains("취소") || lower.contains("삭제") || lower.contains("그만"))) {
            java.util.regex.Matcher idMatcher = java.util.regex.Pattern.compile("(\\d+)").matcher(message);
            return idMatcher.find()
                    ? Map.of("action", "CANCEL", "jobId", Long.parseLong(idMatcher.group(1)))
                    : Map.of("action", "CANCEL");
        }
        String target = lower.contains("힐링존") ? "힐링존" : (lower.contains("수면실") ? "수면실" : "제1열람실");
        return Map.of("action", "WATCH", "targetName", target, "durationMinutes", 90);
    }
}
