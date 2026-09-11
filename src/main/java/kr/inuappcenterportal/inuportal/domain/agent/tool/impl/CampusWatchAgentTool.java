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
        return "학산도서관 힐링존, 수면실, 열람실 실시간 빈자리 감시(스나이퍼)를 등록하거나 현재 감시 목록을 조회/취소합니다. "
                + "(params: {\"action\": \"WATCH\"|\"LIST\"|\"CANCEL\", \"targetName\": \"힐링존\"|\"제1열람실\"|\"노트북실\", \"durationMinutes\": 90}). "
                + "예: '힐링존 자리 나면 알려줘' -> {\"action\": \"WATCH\", \"targetName\": \"힐링존\", \"durationMinutes\": 90}";
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
                    : "힐링존";

            int duration = 90;
            if (params != null && params.get("durationMinutes") != null) {
                try {
                    duration = Integer.parseInt(String.valueOf(params.get("durationMinutes")));
                } catch (Exception ignored) {}
            }

            CampusWatchCreateRequestDto req = new CampusWatchCreateRequestDto(
                    CampusWatchDomain.LIBRARY_SEAT,
                    targetName,
                    targetName,
                    duration
            );

            CampusWatchJobDto job = campusWatchService.registerWatch(member, req);

            UiComponentDto ui = UiComponentDto.of(
                    "CAMPUS_WATCH_RESULT",
                    Map.of(
                            "job", job,
                            "targetName", targetName,
                            "remainingMinutes", job.remainingMinutes()
                    ),
                    "감시 현황 확인하기",
                    "/mypage/notification/smart-watch"
            );

            String summary = String.format(
                    "학산도서관 %s 빈자리 감시를 시작했습니다! 🎯\n최대 %d분 동안 45초마다 안전하게 감시하며, 빈자리가 생기는 즉시 푸시 알림으로 알려드릴게요.",
                    targetName, duration
            );

            return new ToolResult(summary, ui, Map.of("job", job));

        } catch (Exception e) {
            log.error("[CampusWatchAgentTool] 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("빈자리 감시 처리 중 오류가 발생했습니다: " + e.getMessage(), null, null);
        }
    }
}
