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
    public KeywordResponse addKeyword(Member member, String keywordString, Department department, String category) {
        Keyword keyword;

        if (department != null) {
            // 학과 공지 키워드 생성 (기존 설정을 삭제하지 않음)
            keyword = createDepartmentKeyword(member.getId(), keywordString, department);
        } else {
            if (category != null) {
                validateNoticeCategory(category);
            }
            // 학교 공지 키워드 생성 (기존 설정을 삭제하지 않음)
            keyword = createSchoolNoticeKeyword(member.getId(), keywordString, category);
        }

        keywordRepository.save(keyword);
        return KeywordResponse.from(keyword);
    }

    @Transactional
    public void noticeNotifyMatchedUsers(Notice notice) {
        // 1. 키워드 매칭 유저 조회
        List<Keyword> keywordMatches = keywordRepository.findKeywordsByKeywordAndCategoryMatches(notice.getTitle(), notice.getCategory());
        // 2. 카테고리 구독 유저 조회 (키워드 없음)
        List<Keyword> categorySubscribers = keywordRepository.findKeywordsByCategoryAndKeywordIsNull(notice.getCategory());

        // 3. 중복 제거 및 우선순위 적용 (키워드 매칭 우선)
        Map<Long, String> memberIdToTitle = new java.util.HashMap<>();

        // 키워드 매칭자들 먼저 처리 (우선순위 높음)
        for (Keyword k : keywordMatches) {
            if (!memberIdToTitle.containsKey(k.getMemberId())) {
                String title = String.format("[%s-%s] 새로운 공지사항이 등록되었어요.", notice.getCategory(), k.getKeyword());
                memberIdToTitle.put(k.getMemberId(), title);
            }
        }

        // 카테고리 구독자들 처리 (이미 키워드 매칭된 유저는 제외)
        for (Keyword k : categorySubscribers) {
            if (!memberIdToTitle.containsKey(k.getMemberId())) {
                String title = String.format("[%s] 새로운 공지사항이 등록되었어요.", notice.getCategory());
                memberIdToTitle.put(k.getMemberId(), title);
            }
        }

        if (memberIdToTitle.isEmpty()) return;

        // 4. 발송 (타이틀이 서로 다를 수 있으므로 타이틀별로 그룹화하여 발송하거나 개별 발송)
        // 여기서는 효율을 위해 타이틀별로 그룹화
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
            fcmAsyncService.sendAsyncKeywordNotice(tokenMap, title, notice.getTitle(), FcmMessageType.SCHOOL_NOTICE)
        );
    }

    @Transactional
    public void departmentNotifyMatchedUsers(DepartmentNotice departmentNotice, Department department) {
        // 1. 키워드 매칭 유저 조회
        List<Keyword> keywordMatches = keywordRepository.findKeywordsByKeywordAndDepartmentMatches(departmentNotice.getTitle(), department);
        // 2. 학과 구독 유저 조회 (키워드 없음)
        List<Keyword> departmentSubscribers = keywordRepository.findKeywordsByDepartmentAndKeywordIsNull(department);

        // 3. 중복 제거 및 우선순위 적용
        Map<Long, String> memberIdToTitle = new java.util.HashMap<>();

        for (Keyword k : keywordMatches) {
            if (!memberIdToTitle.containsKey(k.getMemberId())) {
                String title = String.format("[%s-%s] 새로운 공지사항이 등록되었어요.", department.getDepartmentName(), k.getKeyword());
                memberIdToTitle.put(k.getMemberId(), title);
            }
        }

        for (Keyword k : departmentSubscribers) {
            if (!memberIdToTitle.containsKey(k.getMemberId())) {
                String title = String.format("[%s] 새로운 공지사항이 등록되었어요.", department.getDepartmentName());
                memberIdToTitle.put(k.getMemberId(), title);
            }
        }

        if (memberIdToTitle.isEmpty()) return;

        // 4. 발송
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
            fcmAsyncService.sendAsyncKeywordNotice(tokenMap, title, departmentNotice.getTitle(), FcmMessageType.DEPARTMENT)
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
        // 키워드가 없는 '학과 전체 알림'만 삭제 (키워드 알림은 보존)
        keywordRepository.deleteByMemberIdAndTypeAndKeywordIsNull(member.getId(), FcmMessageType.DEPARTMENT);

        if (departments == null || departments.isEmpty()) {
            return List.of();
        }

        List<Keyword> newKeywords = departments.stream()
                .distinct()
                .map(dept -> createDepartmentKeyword(member.getId(), null, dept))
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
        // 키워드가 없는 '학교 전체 알림'만 삭제 (키워드 알림은 보존)
        keywordRepository.deleteByMemberIdAndTypeAndKeywordIsNull(member.getId(), FcmMessageType.SCHOOL_NOTICE);

        if (categories == null || categories.isEmpty()) {
            return List.of();
        }

        List<Keyword> newKeywords = categories.stream()
                .distinct()
                .peek(this::validateNoticeCategory)
                .map(category -> createSchoolNoticeKeyword(member.getId(), null, category))
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

    private Keyword createDepartmentKeyword(Long memberId, String keywordString, Department department) {
        return Keyword.builder()
                .memberId(memberId)
                .keyword(keywordString)
                .type(FcmMessageType.DEPARTMENT)
                .department(department)
                .build();
    }

    private Keyword createSchoolNoticeKeyword(Long memberId, String keywordString, String category) {
        return Keyword.builder()
                .memberId(memberId)
                .keyword(keywordString)
                .type(FcmMessageType.SCHOOL_NOTICE)
                .category(category)
                .build();
    }
}
