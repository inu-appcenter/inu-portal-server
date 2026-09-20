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

    private static final Map<String, String> DEPARTMENT_ALIASES = Map.ofEntries(
            Map.entry("컴공", "컴퓨터공학부"),
            Map.entry("컴공과", "컴퓨터공학부"),
            Map.entry("컴퓨터공학과", "컴퓨터공학부"),
            Map.entry("컴퓨터공학", "컴퓨터공학부"),
            Map.entry("임베", "임베디드시스템공학과"),
            Map.entry("임베과", "임베디드시스템공학과"),
            Map.entry("임베디드", "임베디드시스템공학과"),
            Map.entry("임베디드시스템", "임베디드시스템공학과"),
            Map.entry("정통", "정보통신공학과"),
            Map.entry("정통과", "정보통신공학과"),
            Map.entry("정통공", "정보통신공학과"),
            Map.entry("정보통신", "정보통신공학과"),
            Map.entry("정보통신공학", "정보통신공학과"),
            Map.entry("전전", "전자공학과"),
            Map.entry("전자", "전자공학과"),
            Map.entry("전자과", "전자공학과"),
            Map.entry("전자공학", "전자공학과"),
            Map.entry("전기", "전기공학과"),
            Map.entry("전기과", "전기공학과"),
            Map.entry("전기공학", "전기공학과"),
            Map.entry("기계", "기계공학과"),
            Map.entry("기계과", "기계공학과"),
            Map.entry("기계공학", "기계공학과"),
            Map.entry("메카", "메카트로닉스공학과"),
            Map.entry("메카과", "메카트로닉스공학과"),
            Map.entry("안공", "안전공학과"),
            Map.entry("안전공학", "안전공학과"),
            Map.entry("에신", "에너지화학공학과"),
            Map.entry("에너지화학", "에너지화학공학과"),
            Map.entry("산공", "산업경영공학과"),
            Map.entry("산경", "산업경영공학과"),
            Map.entry("산경공", "산업경영공학과"),
            Map.entry("바시", "바이오-로봇시스템공학과"),
            Map.entry("바이오로봇", "바이오-로봇시스템공학과"),
            Map.entry("화학", "화학과"),
            Map.entry("화학과", "화학과"),
            Map.entry("수학", "수학과"),
            Map.entry("수학과", "수학과"),
            Map.entry("물리", "물리학과"),
            Map.entry("물리학과", "물리학과"),
            Map.entry("패디", "패션산업학과"),
            Map.entry("패션산업", "패션산업학과"),
            Map.entry("해양", "해양학과"),
            Map.entry("경영", "경영학부"),
            Map.entry("경영과", "경영학부"),
            Map.entry("경영학과", "경영학부"),
            Map.entry("경제", "경제학과"),
            Map.entry("경제과", "경제학과"),
            Map.entry("경제학과", "경제학과"),
            Map.entry("무역", "무역학부"),
            Map.entry("무역과", "무역학부"),
            Map.entry("무역학부", "무역학부"),
            Map.entry("소비자", "소비자학과"),
            Map.entry("세무회계", "세무회계학과"),
            Map.entry("세무", "세무회계학과"),
            Map.entry("회계", "세무회계학과"),
            Map.entry("행정", "행정학과"),
            Map.entry("행정과", "행정학과"),
            Map.entry("행정학과", "행정학과"),
            Map.entry("정외", "정치외교학과"),
            Map.entry("정외과", "정치외교학과"),
            Map.entry("정치외교", "정치외교학과"),
            Map.entry("정치외교학과", "정치외교학과"),
            Map.entry("미컴", "미디어커뮤니케이션학과"),
            Map.entry("신방", "미디어커뮤니케이션학과"),
            Map.entry("신방과", "미디어커뮤니케이션학과"),
            Map.entry("미디어커뮤니케이션", "미디어커뮤니케이션학과"),
            Map.entry("사복", "사회복지학과"),
            Map.entry("사회복지", "사회복지학과"),
            Map.entry("국문", "국어국문학과"),
            Map.entry("국문과", "국어국문학과"),
            Map.entry("국어국문", "국어국문학과"),
            Map.entry("영문", "영어영문학과"),
            Map.entry("영문과", "영어영문학과"),
            Map.entry("영어영문", "영어영문학과"),
            Map.entry("독문", "독어독문학과"),
            Map.entry("독문과", "독어독문학과"),
            Map.entry("독어독문", "독어독문학과"),
            Map.entry("불문", "불어불문학과"),
            Map.entry("불문과", "불어불문학과"),
            Map.entry("불어", "불어불문학과"),
            Map.entry("불어불문", "불어불문학과"),
            Map.entry("일문", "일어일문학과"),
            Map.entry("일문과", "일어일문학과"),
            Map.entry("일어일문", "일어일문학과"),
            Map.entry("중문", "중어중문학과"),
            Map.entry("중문과", "중어중문학과"),
            Map.entry("중어중문", "중어중문학과"),
            Map.entry("법학", "법학부"),
            Map.entry("법대", "법학부"),
            Map.entry("법학부", "법학부"),
            Map.entry("생명", "생명공학부"),
            Map.entry("생공", "생명공학부"),
            Map.entry("생명공학", "생명공학부"),
            Map.entry("생명과학", "생명과학부"),
            Map.entry("도시공학", "도시공학과"),
            Map.entry("도시행정", "도시행정학과")
    );

    @Override
    public AgentToolDefinition getDefinition() {
        return new AgentToolDefinition("DIRECTORY", "교내 교수, 교직원, 학과 사무실(과사), 행정부서의 전화번호, 이메일, 연구실/사무실 위치를 조회합니다.",
                List.of("학과 사무실(과사) 위치·전화번호·홈페이지 조회", "교수/교직원 연락처·이메일·전화번호·연구실 조회", "행정부서 위치·연락처 조회"),
                List.of("컴퓨터공학부 과사 전화번호 알려줘", "컴공 사무실 어디야?", "홍길동 교수님 연락처 알려줘", "학사지원과 어디야?"),
                List.of("학교 규정이나 행정 절차 설명은 INU_AI_KNOWLEDGE"),
                Map.of("query", AgentToolParameter.string("조회할 학과명, 교수/교직원 성함 또는 부서명 (예: 컴퓨터공학부, 홍길동, 학생지원과)", true)), false, true);
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
                return new ToolResult("조회할 학과명이나 교수님 성함, 부서명을 입력해주세요.", null, null);
            }

            String cleaned = cleanQuery(displayQuery);
            String aliasDept = resolveDepartmentAlias(cleaned);
            if (aliasDept == null) {
                aliasDept = resolveDepartmentAlias(displayQuery);
            }

            boolean officeQuery = isOfficeQuery(displayQuery, cleaned, aliasDept);

            // 1. 학과 사무실 질의(과사, 학과명 등)인 경우: CollegeOfficeContactService 우선 조회
            if (officeQuery) {
                List<CollegeOfficeContactResponse> officeContacts = searchCollegeOfficeContacts(displayQuery, cleaned, aliasDept);
                if (!officeContacts.isEmpty()) {
                    List<DirectoryEntryResponse> directoryEntries = searchDirectoryEntries(displayQuery, cleaned, aliasDept);
                    UiComponentDto component = UiComponentDto.of("DIRECTORY", officeContacts, "교내 전화번호부 전체보기", "/phonebook");
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("'%s' 관련 학과 사무실(과사) 및 교내 연락처 정보입니다.\n\n", displayQuery));
                    
                    sb.append("🏢 [학과 사무실(과사)]\n");
                    for (CollegeOfficeContactResponse c : officeContacts) {
                        sb.append(String.format("• %s (%s)", c.getDepartmentName(), c.getCollegeName()));
                        if (c.getOfficePhoneNumber() != null && !c.getOfficePhoneNumber().isBlank()) {
                            sb.append(String.format(": 📞 %s", c.getOfficePhoneNumber()));
                        }
                        if (c.getOfficeLocation() != null && !c.getOfficeLocation().isBlank()) {
                            sb.append(String.format(" (위치: %s)", c.getOfficeLocation()));
                        }
                        if (c.getHomepageUrl() != null && !c.getHomepageUrl().isBlank()) {
                            sb.append(String.format(" [홈페이지: %s]", c.getHomepageUrl()));
                        }
                        sb.append("\n");
                    }

                    // 참고용 소속 교수진/교직원 목록이 있다면 함께 요약 제공
                    if (!directoryEntries.isEmpty()) {
                        sb.append("\n👨‍🏫 [소속 교수 및 교직원 (참고)]\n");
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
                    }

                    return new ToolResult(sb.toString().trim(), component, officeContacts);
                }
            }

            // 2. 교수/직원 인물 질의이거나, 학과 사무실 결과가 없었던 경우: DirectoryService 조회
            List<DirectoryEntryResponse> directoryEntries = searchDirectoryEntries(displayQuery, cleaned, aliasDept);
            if (!directoryEntries.isEmpty()) {
                UiComponentDto component = UiComponentDto.of("DIRECTORY", directoryEntries, "교내 전화번호부 전체보기", "/phonebook");
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("'%s' 관련 교내 연락처 정보입니다.\n\n", displayQuery));
                sb.append("👨‍🏫 [교수/교직원]\n");
                for (int i = 0; i < Math.min(directoryEntries.size(), 4); i++) {
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

            // 3. 인물 검색 실패 시 학과 사무실(CollegeOfficeContactService) 대체 검색
            List<CollegeOfficeContactResponse> officeContacts = searchCollegeOfficeContacts(displayQuery, cleaned, aliasDept);
            if (!officeContacts.isEmpty()) {
                UiComponentDto component = UiComponentDto.of("DIRECTORY", officeContacts, "교내 전화번호부 전체보기", "/phonebook");
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("'%s' 관련 학과/부서 연락처 정보입니다.\n\n", displayQuery));
                sb.append("🏢 [학과/부서 사무실]\n");
                for (CollegeOfficeContactResponse c : officeContacts) {
                    sb.append(String.format("• %s (%s): 📞 %s", c.getDepartmentName(), c.getCollegeName(), c.getOfficePhoneNumber()));
                    if (c.getOfficeLocation() != null && !c.getOfficeLocation().isBlank()) {
                        sb.append(String.format(" (위치: %s)", c.getOfficeLocation()));
                    }
                    sb.append("\n");
                }
                return new ToolResult(sb.toString().trim(), component, officeContacts);
            }

            // 검색 결과 없음
            UiComponentDto emptyComponent = UiComponentDto.of("DIRECTORY", Collections.emptyList(), "교내 전화번호부 전체보기", "/phonebook");
            String notFoundMsg = String.format("'%s' 관련 교내 연락처를 찾지 못했습니다. 전화번호부 메뉴에서 직접 검색해보세요.", displayQuery);
            return new ToolResult(notFoundMsg, emptyComponent, Collections.emptyList());

        } catch (Exception e) {
            log.error("연락처 도구 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("교내 전화번호부를 검색하는 도중 오류가 발생했습니다.", null, null);
        }
    }

    private boolean isOfficeQuery(String displayQuery, String cleanedQuery, String aliasDept) {
        if (displayQuery == null) return false;
        String lower = displayQuery.toLowerCase(Locale.ROOT);
        if (lower.contains("과사") || lower.contains("사무실") || lower.contains("행정실")
                || lower.contains("학과사무실") || lower.contains("과사무실") || lower.contains("대표번호")
                || lower.contains("대표 번호") || lower.contains("학과번호") || lower.contains("위치")) {
            return true;
        }
        return aliasDept != null;
    }

    private String resolveDepartmentAlias(String query) {
        if (query == null || query.isBlank()) return null;
        String trimmed = query.replaceAll("\\s+", "");
        if (DEPARTMENT_ALIASES.containsKey(trimmed)) {
            return DEPARTMENT_ALIASES.get(trimmed);
        }
        for (Map.Entry<String, String> entry : DEPARTMENT_ALIASES.entrySet()) {
            if (trimmed.equals(entry.getKey()) || trimmed.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private List<DirectoryEntryResponse> searchDirectoryEntries(String originalQuery, String cleanedQuery, String aliasDept) {
        Set<String> searchQueries = new LinkedHashSet<>();
        if (originalQuery != null && !originalQuery.isBlank()) searchQueries.add(originalQuery);
        if (cleanedQuery != null && !cleanedQuery.isBlank()) searchQueries.add(cleanedQuery);
        if (aliasDept != null && !aliasDept.isBlank()) searchQueries.add(aliasDept);

        for (String q : searchQueries) {
            ListResponseDto<DirectoryEntryResponse> result = directoryService.getEntries(null, q, 1);
            if (result != null && result.getContents() != null && !result.getContents().isEmpty()) {
                return result.getContents();
            }
        }
        return Collections.emptyList();
    }

    private List<CollegeOfficeContactResponse> searchCollegeOfficeContacts(String originalQuery, String cleanedQuery, String aliasDept) {
        Set<String> searchQueries = new LinkedHashSet<>();
        if (aliasDept != null && !aliasDept.isBlank()) searchQueries.add(aliasDept);
        if (cleanedQuery != null && !cleanedQuery.isBlank()) searchQueries.add(cleanedQuery);
        if (originalQuery != null && !originalQuery.isBlank()) searchQueries.add(originalQuery);

        for (String q : searchQueries) {
            ListResponseDto<CollegeOfficeContactResponse> result = collegeOfficeContactService.getContacts(null, q, 1);
            if (result != null && result.getContents() != null && !result.getContents().isEmpty()) {
                return result.getContents();
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
        boolean changed = true;
        while (changed && cleaned.length() >= 2) {
            Matcher matcher = TITLE_SUFFIX_PATTERN.matcher(cleaned);
            if (matcher.find()) {
                String stripped = cleaned.substring(0, matcher.start()).trim();
                if (stripped.length() >= 2) {
                    cleaned = stripped;
                } else {
                    changed = false;
                }
            } else {
                changed = false;
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
