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

        // 클라이언트 단말에서 안전하게 LMS REST API(Moodle WebService)를 직접 호출하도록 지시
        UiComponentDto ui = UiComponentDto.of(
                "LMS_ACTION",
                Map.of(
                        "target", target,
                        "courseName", courseName,
                        "message", "사이버캠퍼스(LMS) 실시간 과제 및 강의 연동"
                ),
                "사이버캠퍼스 바로가기",
                "https://lms.inu.ac.kr"
        );

        String summary = String.format("사이버캠퍼스(LMS)의 %s 정보를 확인하기 위해 안전한 연동 세션을 연결합니다. 학생 스마트폰 앱의 보안 영역에서 과제 마감 및 수강 현황이 직접 조회됩니다.",
                "COURSES".equalsIgnoreCase(target) ? "수강 강좌" : (!courseName.isBlank() ? courseName + " 과제" : "다가오는 과제 마감 일정"));

        return new ToolResult(summary, ui, Map.of(
                "clientAction", "EXECUTE_LMS_ACTION",
                "target", target,
                "courseName", courseName
        ));
    }
}
