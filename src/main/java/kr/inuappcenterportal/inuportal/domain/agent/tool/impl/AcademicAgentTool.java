package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 포털 SSO 학적 정보 및 취득 학점 조회 Agent Tool (Client-Side Action 연동)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AcademicAgentTool implements AgentTool {

    @Override
    public String getName() {
        return "ACADEMIC";
    }

    @Override
    public String getDescription() {
        return "학생 본인의 포털 SSO 학적 정보, 취득 학점, 평점 평균(GPA), 이수 학기, 학적 상태(재학/휴학), 졸업 요건 이수 현황 질문 (params: 없음)";
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        // [클라이언트 사이드 위임]: 개인정보보호법 준수를 위해 서버에 비밀번호를 저장하지 않고,
        // 모바일 기기 하드웨어 보안 영역(KeyStore)에서 학교 포털을 직접 조회하도록 클라이언트 액션 카드 전달
        UiComponentDto ui = UiComponentDto.of(
                "PORTAL_AUTH_REQUIRED",
                Map.of("action", "FETCH_ACADEMIC_INFO", "message", "포털 보안 연동 필요"),
                "포털 학적 정보 연동",
                "/labs/portal/basic-info"
        );

        String summary = "학적 정보 및 취득 학점 조회를 위해 학교 포털 보안 세션에 연결을 준비합니다. 스마트폰 INTIP 앱의 보안 영역에서 안전하게 직접 조회됩니다.";
        return new ToolResult(summary, ui, Map.of("clientAction", "FETCH_ACADEMIC_INFO"));
    }
}
