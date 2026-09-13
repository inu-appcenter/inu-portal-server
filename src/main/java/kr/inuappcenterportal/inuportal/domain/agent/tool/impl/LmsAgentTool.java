package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.*;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 인천대학교 사이버캠퍼스(LMS) 과제, 강좌 및 온라인 강의 진도율 조회 Agent Tool (Client-Side Action 연동)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LmsAgentTool implements AgentTool {

    @Override
    public AgentToolDefinition getDefinition() {
        return new AgentToolDefinition("LMS", "기기에서 조회한 사이버캠퍼스 강좌와 학습 일정을 안내합니다.",
                List.of("과제 마감과 미제출 과제 조회", "수강 강좌 목록 조회", "다가오는 LMS 일정 조회", "강좌별 콘텐츠·진도 조회", "강좌 성적 개요 조회", "마감 리마인더 등록"),
                List.of("미제출 과제 있어?", "이번 주 마감 과제 알려줘", "내 LMS 강좌 목록 보여줘", "OO과목 진도 보여줘", "과제 마감 알림 설정해줘"),
                List.of("개인 시간표는 TIMETABLE", "학교 시험기간은 SCHEDULE"),
                Map.of("target", AgentToolParameter.string("조회 대상", false, "ASSIGNMENTS", "COURSES", "UPCOMING", "CONTENTS", "PROGRESS", "GRADES", "REMINDER"),
                        "courseName", AgentToolParameter.string("특정 강좌명", false)), false, true);
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        String target = "ASSIGNMENTS";
        String courseName = "";

        if (params != null) {
            if (params.containsKey("target") && params.get("target") != null) {
                target = String.valueOf(params.get("target")).toUpperCase();
            }
            if (params.containsKey("courseName") && params.get("courseName") != null) {
                courseName = String.valueOf(params.get("courseName")).trim();
            }
        }

        log.info("[LmsAgentTool] execute target: {}, courseName: {}", target, courseName);

        // 클라이언트 기기(SSO)에서 제공된 실제 LMS 과제/강좌 컨텍스트가 존재하는지 확인
        if (params != null && params.containsKey("_clientContext")) {
            Object clientCtxObj = params.get("_clientContext");
            if (clientCtxObj instanceof Map<?, ?> clientCtx) {
                Object lmsObj = clientCtx.get("lms");
                if (lmsObj instanceof Map<?, ?> lmsData && !lmsData.isEmpty()) {
                    List<?> events = (lmsData.get("events") instanceof List<?>) ? (List<?>) lmsData.get("events") : Collections.emptyList();
                    List<?> courses = (lmsData.get("courses") instanceof List<?>) ? (List<?>) lmsData.get("courses") : Collections.emptyList();
                    if (!courseName.isBlank() && !events.isEmpty()) {
                        final String requestedCourseName = courseName;
                        events = events.stream().filter(event -> event instanceof Map<?, ?> map
                                && String.valueOf(map.get("course")).contains(requestedCourseName)).toList();
                    }

                    UiComponentDto ui = UiComponentDto.of(
                            "LMS_ASSIGNMENTS",
                            lmsData,
                            "사이버캠퍼스 바로가기",
                            "https://lms.inu.ac.kr"
                    );

                    StringBuilder sb = new StringBuilder();
                    if (!events.isEmpty()) {
                        sb.append(String.format("사이버캠퍼스(LMS) 마감 예정 과제 및 일정 %d건 조회 성공:\n", events.size()));
                        int count = 0;
                        for (Object evObj : events) {
                            if (evObj instanceof Map<?, ?> ev) {
                                String evName = ev.get("name") != null ? String.valueOf(ev.get("name")) : "과제";
                                Object courseObj = ev.get("course");
                                String cName = (courseObj instanceof Map<?, ?> cm && cm.get("fullname") != null) ? String.valueOf(cm.get("fullname")) : "";
                                sb.append(String.format("- %s%s\n", cName.isBlank() ? "" : "[" + cName + "] ", evName));
                                if (++count >= 5) break;
                            }
                        }
                    } else if (!courses.isEmpty()) {
                        sb.append(String.format("현재 마감 예정 과제는 없으며, 수강 중인 강좌 %d과목이 조회되었습니다.\n", courses.size()));
                    } else {
                        sb.append("현재 2주 이내에 마감 예정인 사이버캠퍼스(LMS) 과제나 일정이 없습니다.\n");
                    }

                    return new ToolResult(sb.toString().trim(), ui, Map.of("lms", lmsData));
                }
            }
        }

        // 클라이언트 단말 세션 정보가 없을 경우 연동 유도 카드 전달
        UiComponentDto ui = UiComponentDto.of(
                "LMS_AUTH_REQUIRED",
                Map.of(
                        "target", target,
                        "courseName", courseName,
                        "message", "사이버캠퍼스(LMS) 계정 연동 필요"
                ),
                "LMS 계정 연동하기",
                "openLmsAccountModal"
        );

        String summary = "사이버캠퍼스(LMS) 과제 마감 일정 및 강좌 조회를 위해 LMS 계정 연동이 필요합니다. 아래 연동 카드를 통해 학번과 비밀번호를 1회 등록하시면 실시간으로 과제와 일정을 바로 확인하실 수 있습니다.";

        return new ToolResult(summary, ui, Map.of("clientAction", "EXECUTE_LMS_ACTION", "target", target, "courseName", courseName));
    }

    @Override
    public boolean supportsFallback(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        return lower.contains("lms") || lower.contains("과제") || lower.contains("사이버캠퍼스") || lower.contains("레포트") || lower.contains("숙제") || lower.contains("온라인 강의") || lower.contains("인강") || lower.contains("진도율") || lower.contains("동영상 강의");
    }

    @Override
    public Map<String, Object> createFallbackParams(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null) return Map.of("target", "ASSIGNMENTS");
        String lower = message.toLowerCase();
        String target = "ASSIGNMENTS";
        if (lower.contains("강좌") || lower.contains("과목") || lower.contains("수강")) {
            target = "COURSES";
        } else if (lower.contains("마감") || lower.contains("다가오는") || lower.contains("남은")) {
            target = "UPCOMING";
        }
        return Map.of("target", target);
    }
}
