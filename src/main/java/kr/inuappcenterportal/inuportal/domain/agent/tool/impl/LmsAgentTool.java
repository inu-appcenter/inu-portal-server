package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 인천대학교 사이버캠퍼스(LMS) 과제, 강좌 및 온라인 강의 진도율 조회 Agent Tool (Client-Side Action 연동)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LmsAgentTool implements AgentTool {

    @Override
    public String getName() {
        return "LMS";
    }

    @Override
    public String getDescription() {
        return "인천대학교 사이버캠퍼스(LMS) 과제 마감 일정, 미제출 과제, 수강 중인 강좌 목록, 주차별 동영상 강의 출결 및 진도율, 시험 일정 관련 질문 (params: {\"target\": \"ASSIGNMENTS\"|\"COURSES\"|\"UPCOMING\", \"courseName\": \"과목명\"})";
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

        // 클라이언트 단말에서 안전하게 LMS REST API(Moodle WebService)를 직접 호출하도록 안내 및 모달 연결
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

        return new ToolResult(summary, ui, Map.of(
                "clientAction", "EXECUTE_LMS_ACTION",
                "target", target,
                "courseName", courseName
        ));
    }
}
