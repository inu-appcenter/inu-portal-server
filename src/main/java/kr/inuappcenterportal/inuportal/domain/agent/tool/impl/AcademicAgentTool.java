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
        // 클라이언트 기기(SSO)에서 제공된 실제 학적 컨텍스트가 존재하는지 확인
        if (params != null && params.containsKey("_clientContext")) {
            Object clientCtxObj = params.get("_clientContext");
            if (clientCtxObj instanceof Map<?, ?> rawCtx) {
                Object academicObj = rawCtx.get("academic");
                if (academicObj instanceof Map<?, ?> rawAcademic && !rawAcademic.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> academicData = (Map<String, Object>) rawAcademic;
                    // 전체 ERP 필드는 인증된 사용자 카드에만 사용한다. INUChat 도구는
                    // academic(비식별 요약)만 선별해 읽으므로 academicDisplay를 외부로 전달하지 않는다.
                    Map<String, Object> displayData = academicData;
                    Object displayObj = rawCtx.get("academicDisplay");
                    if (displayObj instanceof Map<?, ?> rawDisplay && !rawDisplay.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> typedDisplay = (Map<String, Object>) rawDisplay;
                        displayData = typedDisplay;
                    }
                    String name = academicData.get("koreanName") != null ? String.valueOf(academicData.get("koreanName")) : "학우";
                    String dept = academicData.get("departmentName") != null ? String.valueOf(academicData.get("departmentName")) : "소속";
                    String status = academicData.get("enrollmentStatus") != null ? String.valueOf(academicData.get("enrollmentStatus")) : "재학";
                    String credits = academicData.get("acquiredCredits") != null ? String.valueOf(academicData.get("acquiredCredits")) : "0";
                    String gpa = academicData.get("gradeAverage") != null ? String.valueOf(academicData.get("gradeAverage")) : "-";
                    String advisor = academicData.get("advisorProfessorName") != null ? String.valueOf(academicData.get("advisorProfessorName")) : "";
                    String semesters = academicData.get("completedSemesterCount") != null ? String.valueOf(academicData.get("completedSemesterCount")) : "";

                    UiComponentDto ui = UiComponentDto.of(
                            "ACADEMIC_INFO",
                            displayData,
                            "학적 정보 상세보기",
                            "/mypage"
                    );

                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("학생 학적 데이터: 이름 %s, 소속 %s, 상태 %s, 취득학점 %s학점, 평점평균(GPA) %s",
                            name, dept, status, credits, gpa));
                    if (!advisor.isBlank()) {
                        sb.append(String.format(", 지도교수 %s", advisor));
                    }
                    if (!semesters.isBlank()) {
                        sb.append(String.format(", 이수학기 %s학기", semesters));
                    }
                    sb.append(" (기기 보안영역 연동 실시간 조회 성공)");

                    return new ToolResult(sb.toString(), ui, Map.of("academic", academicData));
                }

                // Credentials exist on the device, but the portal/ERP lookup
                // did not produce academic data. Do not tell the user to link
                // the same account again: that hides the real failure state.
                Object portalObj = rawCtx.get("portal");
                if (portalObj instanceof Map<?, ?> portalData
                        && Boolean.TRUE.equals(portalData.get("linked"))) {
                    String errorMessage = portalData.get("academicErrorMessage") != null
                            ? String.valueOf(portalData.get("academicErrorMessage"))
                            : "포털 학적 정보를 가져오지 못했습니다.";
                    UiComponentDto ui = UiComponentDto.of(
                            "ACADEMIC_FETCH_FAILED",
                            Map.of("message", errorMessage)
                    );
                    return new ToolResult(
                            "포털 계정은 연동되어 있지만 학적 정보 조회에 실패했습니다. 잠시 후 다시 시도해주세요.",
                            ui,
                            Map.of("clientAction", "RETRY_FETCH_ACADEMIC_INFO")
                    );
                }
            }
        }

        // 클라이언트 단말 세션 정보가 없을 경우 연동 유도 카드 전달
        UiComponentDto ui = UiComponentDto.of(
                "PORTAL_AUTH_REQUIRED",
                Map.of("action", "FETCH_ACADEMIC_INFO", "message", "포털 보안 연동 필요"),
                "포털 학적 정보 연동",
                "openPortalAccountModal"
        );

        String summary = "학적 정보 및 취득 학점 조회를 위해 학교 포털 계정 연동이 필요합니다. 모바일 앱 환경에서 1회 연동하시면 실시간 학점과 학적이 바로 표시됩니다.";
        return new ToolResult(summary, ui, Map.of("clientAction", "FETCH_ACADEMIC_INFO"));
    }

    @Override
    public boolean supportsFallback(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        boolean isFirstPersonAcademic = (lower.contains("나 ") || lower.startsWith("나") || lower.contains("내 ") || lower.startsWith("내") || lower.contains("저 ")) &&
                (lower.contains("졸업") || lower.contains("수료") || lower.contains("이수") || lower.contains("학점"));
        return isFirstPersonAcademic || lower.contains("학적") || lower.contains("취득 학점") || lower.contains("취득학점") ||
                lower.contains("이수 학점") || lower.contains("이수학점") || lower.contains("내 학점") || lower.contains("gpa") || lower.contains("평점");
    }
}
