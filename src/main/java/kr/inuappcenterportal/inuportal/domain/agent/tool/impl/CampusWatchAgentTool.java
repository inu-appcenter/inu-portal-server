package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.CampusWatchCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.CampusWatchJobDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.enums.CampusWatchDomain;
import kr.inuappcenterportal.inuportal.domain.agent.service.CampusWatchService;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
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
    public String getName() {
        return "ACTION_CAMPUS_WATCH";
    }

    @Override
    public String getDescription() {
        return "학산도서관 열람실/노트북실/힐링존 실시간 빈자리 감시(스나이퍼) 또는 스터디룸 희망 시간대 취소표 감시를 등록하거나 현재 감시 목록을 조회/취소합니다. "
                + "(params: {\"action\": \"WATCH\"|\"LIST\"|\"CANCEL\", \"domain\": \"LIBRARY_SEAT\"|\"STUDY_ROOM\", \"targetName\": \"제1열람실\"|\"205호\", \"seatNo\": \"43\", \"hopeDate\": \"YYYY-MM-DD\", \"targetHour\": 15, \"durationMinutes\": 90}). "
                + "예: '제1열람실 43번 좌석 비면 알려줘' -> {\"action\": \"WATCH\", \"domain\": \"LIBRARY_SEAT\", \"targetName\": \"제1열람실\", \"seatNo\": \"43\"}, "
                + "'내일 3시 스터디룸 205호 자리 나면 알려줘' -> {\"action\": \"WATCH\", \"domain\": \"STUDY_ROOM\", \"targetName\": \"205호\", \"targetHour\": 15}";
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
        return isWatchTarget || isWatchList;
    }

    @Override
    public Map<String, Object> createFallbackParams(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null) return Map.of("action", "LIST");
        String lower = message.toLowerCase();
        if (lower.contains("감시") && (lower.contains("목록") || lower.contains("조회") || lower.contains("현황"))) {
            return Map.of("action", "LIST");
        }
        String target = lower.contains("힐링존") ? "힐링존" : (lower.contains("수면실") ? "수면실" : "제1열람실");
        return Map.of("action", "WATCH", "targetName", target, "durationMinutes", 90);
    }
}
