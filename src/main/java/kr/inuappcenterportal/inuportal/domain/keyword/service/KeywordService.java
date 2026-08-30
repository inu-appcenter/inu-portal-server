package kr.inuappcenterportal.inuportal.domain.keyword.service;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmAsyncService;
import kr.inuappcenterportal.inuportal.domain.keyword.domain.Keyword;
import kr.inuappcenterportal.inuportal.domain.keyword.dto.res.KeywordResponse;
import kr.inuappcenterportal.inuportal.domain.keyword.repository.KeywordRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import kr.inuappcenterportal.inuportal.domain.category.enums.CategoryType;
import kr.inuappcenterportal.inuportal.domain.category.repository.CategoryRepository;
import kr.inuappcenterportal.inuportal.domain.notice.model.Notice;
import kr.inuappcenterportal.inuportal.domain.notice.model.DepartmentNotice;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeywordService {

    private final KeywordRepository keywordRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final FcmAsyncService fcmAsyncService;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<KeywordResponse> getKeywords(Member member) {
        List<Keyword> keywords = keywordRepository.findAllByMemberId(member.getId());
        return keywords.stream().map(KeywordResponse::from).toList();
    }

    @Transactional
    public KeywordResponse addKeyword(Member member, String keywordString, Department department, String category, Boolean isExcluded) {
        String trimmedKeyword = keywordString != null ? keywordString.trim() : null;
        boolean excluded = isExcluded != null && isExcluded;

        // 동일한 키워드 설정이 이미 존재하는지 멱등성(Idempotency) 확인
        List<Keyword> existingKeywords = keywordRepository.findAllByMemberId(member.getId());
        for (Keyword existing : existingKeywords) {
            boolean sameType = department != null 
                    ? (existing.getType() == FcmMessageType.DEPARTMENT && existing.getDepartment() == department)
                    : (existing.getType() == FcmMessageType.SCHOOL_NOTICE && java.util.Objects.equals(existing.getCategory(), category));
            boolean sameKeyword = java.util.Objects.equals(existing.getKeyword(), trimmedKeyword);
            boolean sameExcluded = existing.isExcluded() == excluded;

            if (sameType && sameKeyword && sameExcluded) {
                log.info("[키워드 등록 스킵] 이미 동일한 키워드가 등록되어 있습니다. memberId={}, keyword={}", member.getId(), trimmedKeyword);
                return KeywordResponse.from(existing);
            }
        }

        Keyword keyword;

        if (department != null) {
            // 학과 공지 키워드 생성 (기존 설정을 삭제하지 않음)
            keyword = createDepartmentKeyword(member.getId(), trimmedKeyword, department, excluded);
        } else {
            if (category != null) {
                validateNoticeCategory(category);
            }
            // 학교 공지 키워드 생성 (기존 설정을 삭제하지 않음)
            keyword = createSchoolNoticeKeyword(member.getId(), trimmedKeyword, category, excluded);
        }

        keywordRepository.save(keyword);
        return KeywordResponse.from(keyword);
    }

    @Transactional
    public void noticeNotifyMatchedUsers(Notice notice) {
        // 1. 키워드 매칭 유저 조회 (포함 키워드)
        List<Keyword> keywordMatches = keywordRepository.findKeywordsByKeywordAndCategoryMatches(notice.getTitle(), notice.getCategory());
        // 2. 카테고리 구독 유저 조회 (키워드 없음)
        List<Keyword> categorySubscribers = keywordRepository.findKeywordsByCategoryAndKeywordIsNull(notice.getCategory());
        // 3. 제외 키워드 매칭 유저 조회
        List<Keyword> excludeMatches = keywordRepository.findExcludeKeywordsByCategoryMatches(notice.getTitle(), notice.getCategory());
        java.util.Set<Long> excludedMemberIds = excludeMatches.stream().map(Keyword::getMemberId).collect(Collectors.toSet());

        // 4. 중복 제거 및 우선순위 적용 (포함 키워드 매칭 우선, 카테고리 구독 시 제외 키워드 필터링)
        Map<Long, String> memberIdToTitle = new java.util.HashMap<>();

        // 포함 키워드 매칭자들 먼저 처리 (우선순위 높음)
        for (Keyword k : keywordMatches) {
            if (!memberIdToTitle.containsKey(k.getMemberId())) {
                String title = String.format("[%s-%s] 새로운 공지사항이에요.", notice.getCategory(), k.getKeyword());
                memberIdToTitle.put(k.getMemberId(), title);
            }
        }

        // 카테고리 구독자들 처리 (이미 키워드 매칭된 유저 및 제외 키워드 매칭 유저는 제외)
        for (Keyword k : categorySubscribers) {
            if (!memberIdToTitle.containsKey(k.getMemberId()) && !excludedMemberIds.contains(k.getMemberId())) {
                String title = String.format("[%s] 새로운 공지사항이에요.", notice.getCategory());
                memberIdToTitle.put(k.getMemberId(), title);
            }
        }

        if (memberIdToTitle.isEmpty()) return;

        // 5. 발송 (타이틀이 서로 다를 수 있으므로 타이틀별로 그룹화하여 발송)
        Map<String, Map<String, Long>> titleToTokens = new java.util.HashMap<>();
        
        List<Long> allMemberIds = new java.util.ArrayList<>(memberIdToTitle.keySet());
        List<FcmToken> fcmTokens = fcmTokenRepository.findFcmTokensByMemberIds(allMemberIds);

        for (FcmToken token : fcmTokens) {
            if (token.getMemberId() == null) continue;
            String title = memberIdToTitle.get(token.getMemberId());
            titleToTokens.computeIfAbsent(title, k -> new java.util.HashMap<>())
                         .put(token.getToken(), token.getMemberId());
        }

        titleToTokens.forEach((title, tokenMap) ->
            fcmAsyncService.sendAsyncKeywordNotice(tokenMap, title, notice.getTitle(), FcmMessageType.SCHOOL_NOTICE, notice.getId(), notice.getUrl())
        );
    }

    @Transactional
    public void departmentNotifyMatchedUsers(DepartmentNotice departmentNotice, Department department, Integer scheduleCount) {
        // 1. 키워드 매칭 유저 조회 (포함 키워드)
        List<Keyword> keywordMatches = keywordRepository.findKeywordsByKeywordAndDepartmentMatches(departmentNotice.getTitle(), department);
        // 2. 학과 구독 유저 조회 (키워드 없음)
        List<Keyword> departmentSubscribers = keywordRepository.findKeywordsByDepartmentAndKeywordIsNull(department);
        // 3. 제외 키워드 매칭 유저 조회
        List<Keyword> excludeMatches = keywordRepository.findExcludeKeywordsByDepartmentMatches(departmentNotice.getTitle(), department);
        java.util.Set<Long> excludedMemberIds = excludeMatches.stream().map(Keyword::getMemberId).collect(Collectors.toSet());

        // 4. 중복 제거 및 우선순위 적용 (포함 키워드 매칭 우선, 학과 전체 구독 시 제외 키워드 필터링)
        Map<Long, String> memberIdToTitle = new java.util.HashMap<>();

        for (Keyword k : keywordMatches) {
            if (!memberIdToTitle.containsKey(k.getMemberId())) {
                String title = String.format("[%s-%s] 새로운 학과 공지사항이에요.", department.getDepartmentName(), k.getKeyword());
                memberIdToTitle.put(k.getMemberId(), title);
            }
        }

        for (Keyword k : departmentSubscribers) {
            if (!memberIdToTitle.containsKey(k.getMemberId()) && !excludedMemberIds.contains(k.getMemberId())) {
                String title = String.format("[%s] 새로운 학과 공지사항이에요.", department.getDepartmentName());
                memberIdToTitle.put(k.getMemberId(), title);
            }
        }

        if (memberIdToTitle.isEmpty()) return;

        // 5. 발송
        Map<String, Map<String, Long>> titleToTokens = new java.util.HashMap<>();
        List<Long> allMemberIds = new java.util.ArrayList<>(memberIdToTitle.keySet());
        List<FcmToken> fcmTokens = fcmTokenRepository.findFcmTokensByMemberIds(allMemberIds);

        for (FcmToken token : fcmTokens) {
            if (token.getMemberId() == null) continue;
            String title = memberIdToTitle.get(token.getMemberId());
            titleToTokens.computeIfAbsent(title, k -> new java.util.HashMap<>())
                         .put(token.getToken(), token.getMemberId());
        }

        String body = departmentNotice.getTitle();
        if (scheduleCount != null && scheduleCount > 0) {
            body += String.format("\n[횃불이 AI] 일정 %d개를 캘린더에서 확인해보세요.", scheduleCount);
        }

        final String finalBody = body;
        titleToTokens.forEach((title, tokenMap) ->
            fcmAsyncService.sendAsyncKeywordNotice(tokenMap, title, finalBody, FcmMessageType.DEPARTMENT, departmentNotice.getId(), departmentNotice.getUrl())
        );
    }

    @Transactional
    public void deleteKeyword(Member member, Long keywordId) {
        Keyword keyword = getKeywordById(keywordId);
        validateKeywordOwnership(member.getId(), keywordId);
        keywordRepository.delete(keyword);
    }

    @Transactional(readOnly = true)
    public List<KeywordResponse> getDepartmentFcm(Member member) {
        return keywordRepository.findAllByMemberIdAndKeywordIsNullAndType(member.getId(), FcmMessageType.DEPARTMENT)
                .stream().map(KeywordResponse::from).toList();
    }

    @Transactional
    public List<KeywordResponse> syncDepartmentFcm(Member member, List<Department> departments) {
        // 키워드가 없는 '학과 전체 알림'만 삭제 (키워드 알림 및 제외 키워드는 보존)
        keywordRepository.deleteByMemberIdAndTypeAndKeywordIsNull(member.getId(), FcmMessageType.DEPARTMENT);

        if (departments == null || departments.isEmpty()) {
            return List.of();
        }

        List<Keyword> newKeywords = departments.stream()
                .distinct()
                .map(department -> createDepartmentKeyword(member.getId(), null, department, false))
                .collect(Collectors.toList());

        return keywordRepository.saveAll(newKeywords).stream()
                .map(KeywordResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<KeywordResponse> getNoticeFcm(Member member) {
        return keywordRepository.findAllByMemberIdAndKeywordIsNullAndType(member.getId(), FcmMessageType.SCHOOL_NOTICE)
                .stream().map(KeywordResponse::from).toList();
    }

    @Transactional
    public List<KeywordResponse> syncNoticeFcm(Member member, List<String> categories) {
        // 키워드가 없는 '학교 공지 카테고리 알림'만 삭제 (키워드 알림 및 제외 키워드는 보존)
        keywordRepository.deleteSchoolNoticeByKeywordIsNull(member.getId());

        if (categories == null || categories.isEmpty()) {
            return List.of();
        }

        List<Keyword> newKeywords = categories.stream()
                .distinct()
                .peek(this::validateNoticeCategory)
                .map(category -> createSchoolNoticeKeyword(member.getId(), null, category, false))
                .collect(Collectors.toList());

        return keywordRepository.saveAll(newKeywords).stream()
                .map(KeywordResponse::from)
                .collect(Collectors.toList());
    }

    private void validateNoticeCategory(String category) {
        if (!categoryRepository.existsByCategoryAndType(category, CategoryType.NOTICE)) {
            throw new MyException(MyErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    private Keyword getKeywordById(Long keywordId) {
        return keywordRepository.findById(keywordId)
                .orElseThrow(() -> {
                    log.warn("[Keyword 조회 실패] 존재하지 않는 keywordId Id: {}", keywordId);
                    return new MyException(MyErrorCode.KEYWORD_NOT_FOUND);
                });
    }

    private void validateKeywordOwnership(Long memberId, Long keywordId) {
        if (!keywordRepository.existsByIdAndMemberId(keywordId, memberId)) {
            log.warn("[Keyword 작업 실패] Keyword Id: {}, Member Id: {} - 권한 없음", keywordId, memberId);
            throw new MyException(MyErrorCode.KEYWORD_ACCESS_DENIED);
        }
    }

    private Keyword createDepartmentKeyword(Long memberId, String keywordString, Department department, Boolean isExcluded) {
        return Keyword.builder()
                .memberId(memberId)
                .keyword(keywordString)
                .type(FcmMessageType.DEPARTMENT)
                .department(department)
                .isExcluded(isExcluded)
                .build();
    }

    private Keyword createSchoolNoticeKeyword(Long memberId, String keywordString, String category, Boolean isExcluded) {
        return Keyword.builder()
                .memberId(memberId)
                .keyword(keywordString)
                .type(FcmMessageType.SCHOOL_NOTICE)
                .category(category)
                .isExcluded(isExcluded)
                .build();
    }
}
