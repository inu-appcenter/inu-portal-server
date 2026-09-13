package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.*;
import kr.inuappcenterportal.inuportal.domain.directory.dto.CollegeOfficeContactResponse;
import kr.inuappcenterportal.inuportal.domain.directory.service.CollegeOfficeContactService;
import kr.inuappcenterportal.inuportal.domain.directory.service.DirectoryService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectoryAgentTool implements AgentTool {

    private final CollegeOfficeContactService collegeOfficeContactService;
    private final DirectoryService directoryService;

    @Override
    public AgentToolDefinition getDefinition() {
        return new AgentToolDefinition("DIRECTORY", "교내 학과 사무실과 행정부서 연락처·위치를 조회합니다.",
                List.of("학과 사무실 위치·전화번호 조회", "행정부서 위치·연락처 조회"),
                List.of("컴퓨터공학부 사무실 전화번호 알려줘", "학사지원과 어디야?"),
                List.of("교수 개인 연락처 조회", "학교 규정이나 행정 절차 설명은 INU_AI_KNOWLEDGE"),
                Map.of("query", AgentToolParameter.string("조회할 학과 또는 부서명", true)), false, true);
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        try {
            String query = "";
            if (params != null && params.containsKey("query") && params.get("query") != null) {
                query = String.valueOf(params.get("query")).trim();
            }

            ListResponseDto<CollegeOfficeContactResponse> contactResult =
                    collegeOfficeContactService.getContacts(null, query, 1);

            List<CollegeOfficeContactResponse> list = contactResult.getContents() != null
                    ? contactResult.getContents()
                    : Collections.emptyList();

            UiComponentDto component = UiComponentDto.of("DIRECTORY", list, "교내 전화번호부 전체보기", "/phonebook");

            StringBuilder sb = new StringBuilder();
            if (list.isEmpty()) {
                sb.append(String.format("'%s' 관련 교내 연락처를 찾지 못했습니다. 전화번호부 메뉴에서 직접 검색해보세요.", query));
            } else {
                sb.append(String.format("'%s' 관련 교내 연락처 정보입니다.\n", query));
                for (int i = 0; i < Math.min(list.size(), 3); i++) {
                    CollegeOfficeContactResponse c = list.get(i);
                    sb.append(String.format("• %s (%s): 📞 %s\n", c.getDepartmentName(), c.getCollegeName(), c.getOfficePhoneNumber()));
                }
            }

            return new ToolResult(sb.toString().trim(), component, list);
        } catch (Exception e) {
            log.error("연락처 도구 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("교내 전화번호부를 검색하는 도중 오류가 발생했습니다.", null, null);
        }
    }

    @Override
    public boolean supportsFallback(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        return lower.contains("전화") || lower.contains("번호") || lower.contains("과사") || lower.contains("사무실") || lower.contains("연락처");
    }

    @Override
    public Map<String, Object> createFallbackParams(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        return Map.of("query", message != null ? message.trim() : "");
    }
}
