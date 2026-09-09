package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.domain.keyword.dto.res.KeywordResponse;
import kr.inuappcenterportal.inuportal.domain.keyword.service.KeywordService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeKeywordAgentTool implements AgentTool {

    private final KeywordService keywordService;

    @Override
    public String getName() {
        return "ACTION_NOTICE_KEYWORD";
    }

    @Override
    public String getDescription() {
        return "스마트 공지 키워드 알림 등록 (params: {\"keyword\": \"정제된명사키워드\", \"targetType\": \"SCHOOL\"|\"DEPARTMENT\", \"category\": \"장학\"|\"학사\"|\"모집\"|\"일반\", \"isExcluded\": false|true}). 구어체(예: '학비 지원')는 공식 명사(예: '장학금')로 변환하고, 전공/졸업 관련은 targetType: \"DEPARTMENT\", 전교생 대상은 \"SCHOOL\"로 설정하세요.";
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        if (member == null) {
            return new ToolResult("공지 키워드 알림을 등록하려면 로그인이 필요합니다.",
                    UiComponentDto.of("AUTH_REQUIRED", Map.of(), "로그인하기", "/login"), null);
        }

        try {
            String keyword = (params != null && params.get("keyword") != null)
                    ? String.valueOf(params.get("keyword")).trim()
                    : "";

            if (keyword.isBlank()) {
                return new ToolResult("등록할 알림 키워드를 찾지 못했습니다. 어떤 키워드로 알림을 등록할지 말씀해주세요.", null, null);
            }

            String targetType = (params != null && params.get("targetType") != null)
                    ? String.valueOf(params.get("targetType")).toUpperCase().trim()
                    : "SCHOOL";

            String category = (params != null && params.get("category") != null)
                    ? String.valueOf(params.get("category")).trim()
                    : "전체";

            boolean isExcluded = false;
            if (params != null && params.containsKey("isExcluded")) {
                Object val = params.get("isExcluded");
                if (val instanceof Boolean b) {
                    isExcluded = b;
                } else {
                    isExcluded = Boolean.parseBoolean(String.valueOf(val));
                }
            }

            Department targetDept = null;
            if ("DEPARTMENT".equalsIgnoreCase(targetType) || "DEPT".equalsIgnoreCase(targetType)) {
                targetDept = member.getDepartment();
            }

            KeywordResponse saved = keywordService.addKeyword(member, keyword, targetDept, category, isExcluded);

            String targetName = (targetDept != null)
                    ? targetDept.getDepartmentName() + " 학과공지"
                    : "학교 전체공지 (" + category + ")";

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", saved.keywordId());
            data.put("keyword", saved.keyword());
            data.put("targetType", (targetDept != null) ? "DEPARTMENT" : "SCHOOL");
            data.put("targetName", targetName);
            data.put("category", category);
            data.put("isExcluded", isExcluded);
            data.put("statusText", isExcluded ? "제외 키워드 등록 완료" : "알림 키워드 등록 완료");

            UiComponentDto component = UiComponentDto.of("KEYWORD_CONFIRM", data, "키워드 알림 목록 관리", "/home/notice");

            String summary = isExcluded
                    ? String.format("[%s] 키워드가 %s의 알림 제외 키워드로 등록되었습니다. 해당 단어가 포함된 공지는 알림에서 제외됩니다.", keyword, targetName)
                    : String.format("'%s' 키워드가 %s 알림으로 등록되었습니다! 새로운 공지가 올라오면 바로 푸시를 보내드릴게요.", keyword, targetName);

            return new ToolResult(summary, component, data);
        } catch (Exception e) {
            log.error("공지 키워드 등록 오류: {}", e.getMessage(), e);
            return new ToolResult("공지 키워드를 등록하는 도중 오류가 발생했습니다.", null, null);
        }
    }
}
