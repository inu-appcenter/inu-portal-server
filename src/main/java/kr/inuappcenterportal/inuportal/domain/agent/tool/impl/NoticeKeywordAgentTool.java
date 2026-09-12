package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.category.enums.CategoryType;
import kr.inuappcenterportal.inuportal.domain.category.model.Category;
import kr.inuappcenterportal.inuportal.domain.category.repository.CategoryRepository;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.domain.keyword.dto.res.KeywordResponse;
import kr.inuappcenterportal.inuportal.domain.keyword.service.KeywordService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeKeywordAgentTool implements AgentTool {

    private final KeywordService keywordService;
    private final CategoryRepository categoryRepository;

    @Override
    public String getName() {
        return "ACTION_NOTICE_KEYWORD";
    }

    @Override
    public String getDescription() {
        return "스마트 공지 키워드 알림 등록 (params: {\"keyword\": \"정제된명사키워드\", \"targetType\": \"SCHOOL\"|\"DEPARTMENT\", \"category\": \"장학금\"|\"학사\"|\"일반/행사/모집\"|\"등록금 납부\"|null, \"isExcluded\": false|true}). "
                + "구어체(예: '학비 지원')는 공식 명사(예: '장학금')로 변환하고, 전공/졸업/학과 관련은 targetType: \"DEPARTMENT\", 학교 전체 대상은 \"SCHOOL\"로 설정하세요. "
                + "특정 카테고리에 한정하지 않거나 전체 공지 대상이면 category를 생략하거나 null로 설정하세요.";
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

            String rawCategory = (params != null && params.get("category") != null)
                    ? String.valueOf(params.get("category")).trim()
                    : null;

            String resolvedCategory = resolveNoticeCategory(rawCategory);

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

            // 학과 키워드는 카테고리 없이 학과 단위로 등록됨
            KeywordResponse saved = keywordService.addKeyword(
                    member,
                    keyword,
                    targetDept,
                    targetDept != null ? null : resolvedCategory,
                    isExcluded
            );

            String targetName;
            String manageRoute;
            if (targetDept != null) {
                targetName = targetDept.getDepartmentName() + " 학과공지";
                manageRoute = "/mypage/notification/daily-brief?tab=dept";
            } else if (resolvedCategory != null) {
                targetName = "학교 공지 (" + resolvedCategory + ")";
                manageRoute = "/mypage/notification/daily-brief?tab=school";
            } else {
                targetName = "학교 전체공지";
                manageRoute = "/mypage/notification/daily-brief?tab=school";
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", saved.keywordId());
            data.put("keyword", saved.keyword());
            data.put("targetType", (targetDept != null) ? "DEPARTMENT" : "SCHOOL");
            data.put("targetName", targetName);
            data.put("category", resolvedCategory != null ? resolvedCategory : "전체");
            data.put("isExcluded", isExcluded);
            data.put("statusText", isExcluded ? "제외 키워드 등록 완료" : "알림 키워드 등록 완료");

            UiComponentDto component = UiComponentDto.of("KEYWORD_CONFIRM", data, "키워드 알림 목록 관리", manageRoute);

            String summary = isExcluded
                    ? String.format("[%s] 키워드가 %s의 알림 제외 키워드로 등록되었습니다. 해당 단어가 포함된 공지는 알림에서 제외됩니다.", keyword, targetName)
                    : String.format("'%s' 키워드가 %s 알림으로 등록되었습니다! 새로운 공지가 올라오면 바로 푸시를 보내드릴게요.", keyword, targetName);

            return new ToolResult(summary, component, data);
        } catch (MyException e) {
            log.error("공지 키워드 등록 비즈니스 오류: errorCode={}, message={}",
                    e.getErrorCode(), e.getErrorCode() != null ? e.getErrorCode().getMessage() : "null", e);
            String errorMsg = e.getErrorCode() != null ? e.getErrorCode().getMessage() : "공지 키워드 등록 중 오류가 발생했습니다.";
            return new ToolResult("공지 키워드를 등록하지 못했습니다: " + errorMsg, null, null);
        } catch (Exception e) {
            log.error("공지 키워드 등록 오류: {}", e.getMessage(), e);
            return new ToolResult("공지 키워드를 등록하는 도중 오류가 발생했습니다.", null, null);
        }
    }

    private String resolveNoticeCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            return null;
        }
        String clean = rawCategory.trim();
        String normalized = clean.replaceAll("\\s+", "").toLowerCase();

        if (normalized.equals("전체") || normalized.equals("all") || normalized.equals("학교")
                || normalized.equals("공지") || normalized.equals("none") || normalized.equals("null")) {
            return null;
        }

        try {
            List<Category> noticeCategories = categoryRepository.findAllByType(CategoryType.NOTICE);

            // 1. 완전 일치 또는 공백 무시 일치
            for (Category c : noticeCategories) {
                if (c.getCategory().equalsIgnoreCase(clean)
                        || c.getCategory().replaceAll("\\s+", "").equalsIgnoreCase(normalized)) {
                    return c.getCategory();
                }
            }

            // 2. 부분 일치 (예: "장학" -> "장학금", "모집" -> "일반/행사/모집")
            for (Category c : noticeCategories) {
                String catName = c.getCategory();
                String catNormalized = catName.replaceAll("\\s+", "").toLowerCase();
                if (catNormalized.contains(normalized) || normalized.contains(catNormalized)) {
                    return catName;
                }
            }

            // 3. 주요 표준 공지 카테고리 휴리스틱 매핑
            if (normalized.contains("장학")) {
                return findExistingCategoryOrFallback(noticeCategories, "장학금");
            }
            if (normalized.contains("학사")) {
                return findExistingCategoryOrFallback(noticeCategories, "학사");
            }
            if (normalized.contains("모집") || normalized.contains("행사") || normalized.contains("일반")) {
                return findExistingCategoryOrFallback(noticeCategories, "일반/행사/모집");
            }
            if (normalized.contains("등록금") || normalized.contains("납부")) {
                return findExistingCategoryOrFallback(noticeCategories, "등록금 납부");
            }
            if (normalized.contains("학점") || normalized.contains("교류")) {
                return findExistingCategoryOrFallback(noticeCategories, "학점교류");
            }
            if (normalized.contains("시험") || normalized.contains("교육")) {
                return findExistingCategoryOrFallback(noticeCategories, "교육시험");
            }
            if (normalized.contains("봉사")) {
                return findExistingCategoryOrFallback(noticeCategories, "봉사");
            }
        } catch (Exception e) {
            log.warn("공지 카테고리 매칭 중 예외 발생, 전체 공지(null)로 대체: {}", e.getMessage());
        }

        // 카테고리 테이블에 없는 값은 안전하게 null(학교 전체 공지 대상)로 처리
        return null;
    }

    private String findExistingCategoryOrFallback(List<Category> categories, String targetName) {
        for (Category c : categories) {
            if (c.getCategory().equals(targetName)) {
                return c.getCategory();
            }
        }
        return null;
    }

    @Override
    public boolean supportsFallback(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        return lower.contains("키워드") && (lower.contains("알림") || lower.contains("등록") || lower.contains("추가"));
    }

    @Override
    public Map<String, Object> createFallbackParams(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        return Map.of("keyword", message != null ? message.trim() : "");
    }
}
