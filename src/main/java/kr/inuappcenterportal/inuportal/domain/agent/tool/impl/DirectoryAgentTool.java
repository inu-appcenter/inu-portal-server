package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.*;
import kr.inuappcenterportal.inuportal.domain.directory.dto.CollegeOfficeContactResponse;
import kr.inuappcenterportal.inuportal.domain.directory.dto.DirectoryEntryResponse;
import kr.inuappcenterportal.inuportal.domain.directory.service.CollegeOfficeContactService;
import kr.inuappcenterportal.inuportal.domain.directory.service.DirectoryService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectoryAgentTool implements AgentTool {

    private final CollegeOfficeContactService collegeOfficeContactService;
    private final DirectoryService directoryService;

    private static final Pattern TITLE_SUFFIX_PATTERN = Pattern.compile(
            "(?:교수님|교수|선생님|조교님|학부장님|학과장님|과사|연구실|사무실|연락처|전화번호|이메일|메일|번호)$"
    );

    private static final Pattern HISTORY_NAME_PATTERN = Pattern.compile(
            "([가-힣]{2,4})\\s*(?:교수님|교수|선생님)"
    );

    @Override
    public AgentToolDefinition getDefinition() {
        return new AgentToolDefinition("DIRECTORY", "교내 교수, 교직원, 학과 사무실, 행정부서의 전화번호, 이메일, 연구실/사무실 위치를 조회합니다.",
                List.of("교수/교직원 연락처·이메일·전화번호·연구실 조회", "학과 사무실 위치·전화번호 조회", "행정부서 위치·연락처 조회"),
                List.of("박문주 교수님 연락처 알려줘", "컴퓨터공학부 사무실 전화번호 알려줘", "학사지원과 어디야?", "교수님 이메일 찾아줘"),
                List.of("학교 규정이나 행정 절차 설명은 INU_AI_KNOWLEDGE"),
                Map.of("query", AgentToolParameter.string("조회할 교수/교직원 성함, 학과명 또는 부서명", true)), false, true);
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        try {
            String rawQuery = "";
            if (params != null && params.containsKey("query") && params.get("query") != null) {
                rawQuery = String.valueOf(params.get("query")).trim();
            }

            // query가 비어있거나 '전화번호', '연락처' 등 일반 지시어만 있는 경우 _clientContext에서 지도교수명 확인
            String resolvedQuery = resolveTargetQuery(rawQuery, params);
            String displayQuery = resolvedQuery.isBlank() ? rawQuery : resolvedQuery;

            if (displayQuery.isBlank()) {
                return new ToolResult("조회할 교수님 성함이나 학과/부서명을 입력해주세요.", null, null);
            }

            String cleaned = cleanQuery(displayQuery);

            // 1. DirectoryService (교수, 직원, 교내 전체 전화번호부) 우선 검색
            List<DirectoryEntryResponse> directoryEntries = searchDirectoryEntries(displayQuery, cleaned);

            if (!directoryEntries.isEmpty()) {
                UiComponentDto component = UiComponentDto.of("DIRECTORY", directoryEntries, "교내 전화번호부 전체보기", "/phonebook");
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("'%s' 관련 교내 연락처 정보입니다.\n", displayQuery));
                for (int i = 0; i < Math.min(directoryEntries.size(), 3); i++) {
                    DirectoryEntryResponse d = directoryEntries.get(i);
                    String affiliation = d.getAffiliation() != null ? d.getAffiliation() : "";
                    if (d.getDetailAffiliation() != null && !d.getDetailAffiliation().isBlank()) {
                        affiliation = affiliation.isBlank() ? d.getDetailAffiliation() : affiliation + " " + d.getDetailAffiliation();
                    }
                    String title = d.getName() != null ? d.getName() : "";
                    if (d.getPosition() != null && !d.getPosition().isBlank()) {
                        title = title + " (" + d.getPosition() + ")";
                    }
                    sb.append(String.format("• %s [%s]", title, affiliation));
                    if (d.getPhoneNumber() != null && !d.getPhoneNumber().isBlank()) {
                        sb.append(String.format(": 📞 %s", d.getPhoneNumber()));
                    }
                    if (d.getEmail() != null && !d.getEmail().isBlank()) {
                        sb.append(String.format(", ✉️ %s", d.getEmail()));
                    }
                    sb.append("\n");
                }
                return new ToolResult(sb.toString().trim(), component, directoryEntries);
            }

            // 2. 단과대학 학과사무실 연락처 검색 (과사/행정실 질의 대응)
            List<CollegeOfficeContactResponse> officeContacts = searchCollegeOfficeContacts(displayQuery, cleaned);
            if (!officeContacts.isEmpty()) {
                UiComponentDto component = UiComponentDto.of("DIRECTORY", officeContacts, "교내 전화번호부 전체보기", "/phonebook");
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("'%s' 관련 학과/부서 연락처 정보입니다.\n", displayQuery));
                for (int i = 0; i < Math.min(officeContacts.size(), 3); i++) {
                    CollegeOfficeContactResponse c = officeContacts.get(i);
                    sb.append(String.format("• %s (%s): 📞 %s\n", c.getDepartmentName(), c.getCollegeName(), c.getOfficePhoneNumber()));
                }
                return new ToolResult(sb.toString().trim(), component, officeContacts);
            }

            // 3. 검색 결과 없음
            UiComponentDto emptyComponent = UiComponentDto.of("DIRECTORY", Collections.emptyList(), "교내 전화번호부 전체보기", "/phonebook");
            String notFoundMsg = String.format("'%s' 관련 교내 연락처를 찾지 못했습니다. 전화번호부 메뉴에서 직접 검색해보세요.", displayQuery);
            return new ToolResult(notFoundMsg, emptyComponent, Collections.emptyList());

        } catch (Exception e) {
            log.error("연락처 도구 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("교내 전화번호부를 검색하는 도중 오류가 발생했습니다.", null, null);
        }
    }

    private List<DirectoryEntryResponse> searchDirectoryEntries(String originalQuery, String cleanedQuery) {
        ListResponseDto<DirectoryEntryResponse> result = directoryService.getEntries(null, originalQuery, 1);
        if (result.getContents() != null && !result.getContents().isEmpty()) {
            return result.getContents();
        }

        if (cleanedQuery != null && !cleanedQuery.isBlank() && !cleanedQuery.equals(originalQuery)) {
            ListResponseDto<DirectoryEntryResponse> cleanedResult = directoryService.getEntries(null, cleanedQuery, 1);
            if (cleanedResult.getContents() != null && !cleanedResult.getContents().isEmpty()) {
                return cleanedResult.getContents();
            }
        }
        return Collections.emptyList();
    }

    private List<CollegeOfficeContactResponse> searchCollegeOfficeContacts(String originalQuery, String cleanedQuery) {
        ListResponseDto<CollegeOfficeContactResponse> result = collegeOfficeContactService.getContacts(null, originalQuery, 1);
        if (result.getContents() != null && !result.getContents().isEmpty()) {
            return result.getContents();
        }

        if (cleanedQuery != null && !cleanedQuery.isBlank() && !cleanedQuery.equals(originalQuery)) {
            ListResponseDto<CollegeOfficeContactResponse> cleanedResult = collegeOfficeContactService.getContacts(null, cleanedQuery, 1);
            if (cleanedResult.getContents() != null && !cleanedResult.getContents().isEmpty()) {
                return cleanedResult.getContents();
            }
        }
        return Collections.emptyList();
    }

    private String resolveTargetQuery(String rawQuery, Map<String, Object> params) {
        String trimmed = rawQuery != null ? rawQuery.trim() : "";
        boolean isGeneric = isGenericContactTerm(trimmed);

        if ((trimmed.isBlank() || isGeneric) && params != null && params.containsKey("_clientContext")) {
            Object clientCtxObj = params.get("_clientContext");
            if (clientCtxObj instanceof Map<?, ?> clientCtx) {
                String advisorName = extractAdvisorFromClientContext(clientCtx);
                if (advisorName != null && !advisorName.isBlank()) {
                    return advisorName;
                }
            }
        }
        return trimmed;
    }

    private String extractAdvisorFromClientContext(Map<?, ?> clientCtx) {
        Object displayObj = clientCtx.get("academicDisplay");
        if (displayObj instanceof Map<?, ?> displayMap) {
            Object adv = displayMap.get("advisorProfessorName");
            if (adv != null && !String.valueOf(adv).isBlank()) {
                return String.valueOf(adv).trim();
            }
        }
        Object academicObj = clientCtx.get("academic");
        if (academicObj instanceof Map<?, ?> academicMap) {
            Object adv = academicMap.get("advisorProfessorName");
            if (adv != null && !String.valueOf(adv).isBlank()) {
                return String.valueOf(adv).trim();
            }
        }
        return null;
    }

    private boolean isGenericContactTerm(String term) {
        if (term == null || term.isBlank()) return true;
        String normalized = term.replaceAll("\\s+", "");
        return normalized.equals("전화번호") || normalized.equals("이메일") || normalized.equals("연락처")
                || normalized.equals("번호") || normalized.equals("연구실") || normalized.equals("지도교수")
                || normalized.equals("지도교수님") || normalized.equals("교수님") || normalized.equals("교수")
                || normalized.contains("전화번호나이메일") || normalized.contains("이메일이나전화번호");
    }

    private String cleanQuery(String query) {
        if (query == null) return "";
        String cleaned = query.trim();
        Matcher matcher = TITLE_SUFFIX_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            String stripped = cleaned.substring(0, matcher.start()).trim();
            if (stripped.length() >= 2) {
                return stripped;
            }
        }
        return cleaned;
    }

    private static final Set<String> NON_NAME_PREFIXES = Set.of(
            "지도", "담임", "전담", "학과", "학부", "담당", "우리", "해당", "어떤", "무슨", "소속", "모든"
    );

    @Override
    public boolean supportsFallback(String message, java.util.List<ChatMessageDto> history) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        return lower.contains("전화") || lower.contains("번호") || lower.contains("과사")
                || lower.contains("사무실") || lower.contains("연락처") || lower.contains("이메일")
                || lower.contains("연구실");
    }

    @Override
    public Map<String, Object> createFallbackParams(String message, java.util.List<ChatMessageDto> history) {
        String query = message != null ? message.trim() : "";
        if (isGenericContactTerm(query) && history != null && !history.isEmpty()) {
            // 대화 내역(history)에서 가장 최근 언급된 실제 교수 성함 추출
            for (int i = history.size() - 1; i >= 0; i--) {
                ChatMessageDto chat = history.get(i);
                if (chat.content() != null) {
                    Matcher m = HISTORY_NAME_PATTERN.matcher(chat.content());
                    while (m.find()) {
                        String candidate = m.group(1).trim();
                        if (!NON_NAME_PREFIXES.contains(candidate)) {
                            return Map.of("query", candidate);
                        }
                    }
                }
            }
        }

        // 호칭 제거 후 2글자 이상이면 정제된 query 반환
        String cleaned = cleanQuery(query);
        return Map.of("query", cleaned.length() >= 2 ? cleaned : query);
    }
}
