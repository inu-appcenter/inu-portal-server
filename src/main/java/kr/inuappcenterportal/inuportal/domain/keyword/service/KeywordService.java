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
            // 학과 공지 키워드 생성
            keyword = createDepartmentKeyword(member.getId(), keywordString, department);
        } else {
            if (category != null) {
                validateNoticeCategory(category);
                // SCHOOL_NOTICE 타입, 특정 카테고리 일치, keyword가 null인 데이터 삭제
                keywordRepository.deleteSchoolNoticeByCategoryAndKeywordIsNull(member.getId(), category);
            } else {
                // SCHOOL_NOTICE 타입, keyword가 null인 모든 데이터 삭제
                keywordRepository.deleteSchoolNoticeByKeywordIsNull(member.getId());
            }
            // 학교 공지 키워드 생성
            keyword = createSchoolNoticeKeyword(member.getId(), keywordString, category);
        }

        keywordRepository.save(keyword);
        return KeywordResponse.from(keyword);
    }

    @Transactional
    public void noticeNotifyMatchedUsersAndKeyword(Notice notice) {
        List<Long> memberIds = keywordRepository
                .findMemberIdsByKeywordAndCategoryMatches(notice.getTitle(), notice.getCategory());
        if (memberIds.isEmpty()) return;

        List<FcmToken> fcmTokens = fcmTokenRepository.findFcmTokensByMemberIds(memberIds);

        Map<String, Long> tokenAndMemberId = fcmTokens.stream()
                .filter(t -> t.getMemberId() != null)
                .collect(Collectors.toMap(FcmToken::getToken, FcmToken::getMemberId));

        String title = String.format("[%s] 키워드에 맞는 새 학교 공지사항이 등록되었어요.", notice.getCategory());
        fcmAsyncService.sendAsyncKeywordNotice(tokenAndMemberId, title, notice.getTitle());
    }

    @Transactional
    public void noticeNotifyMatchedUsers(Notice notice) {
        List<Long> memberIds = keywordRepository.findMemberIdsByCategoryAndKeywordIsNull(notice.getCategory());
        if (memberIds.isEmpty()) return;

        List<FcmToken> fcmTokens = fcmTokenRepository.findFcmTokensByMemberIds(memberIds);

        Map<String, Long> tokenAndMemberId = fcmTokens.stream()
                .filter(t -> t.getMemberId() != null)
                .collect(Collectors.toMap(FcmToken::getToken, FcmToken::getMemberId));

        String title = String.format("[%s] 새로운 학교 공지사항이 등록되었어요.", notice.getCategory());
        fcmAsyncService.sendAsyncKeywordNotice(tokenAndMemberId, title, notice.getTitle());
    }

    @Transactional
    public void departmentNotifyMatchedUsersAndKeyword(DepartmentNotice departmentNotice, Department department) {
        List<Long> memberIds = keywordRepository
                .findMemberIdsByKeywordAndDepartmentMatches(departmentNotice.getTitle(), department);
        if (memberIds.isEmpty()) return;

        List<FcmToken> fcmTokens = fcmTokenRepository.findFcmTokensByMemberIds(memberIds);

        Map<String, Long> tokenAndMemberId = fcmTokens.stream()
                .filter(t -> t.getMemberId() != null)
                .collect(Collectors.toMap(FcmToken::getToken, FcmToken::getMemberId));

        fcmAsyncService.sendAsyncKeywordNotice(tokenAndMemberId, "[" + department.getDepartmentName() + "] 키워드에 맞는 새 공지사항이 등록되었습니다.", departmentNotice.getTitle());
    }

    @Transactional
    public void departmentNotifyMatchedUsers(DepartmentNotice departmentNotice, Department department) {
        List<Long> memberIds = keywordRepository.findMemberIdsByDepartmentAndKeywordIsNull(department);
        if (memberIds.isEmpty()) return;

        List<FcmToken> fcmTokens = fcmTokenRepository.findFcmTokensByMemberIds(memberIds);

        Map<String, Long> tokenAndMemberId = fcmTokens.stream()
                .filter(t -> t.getMemberId() != null)
                .collect(Collectors.toMap(FcmToken::getToken, FcmToken::getMemberId));

        fcmAsyncService.sendAsyncKeywordNotice(tokenAndMemberId, "[" + department.getDepartmentName() + "] 새로운 공지사항이 등록되었습니다.", departmentNotice.getTitle());
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
        keywordRepository.deleteAllByMemberIdAndType(member.getId(), FcmMessageType.DEPARTMENT);

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
        keywordRepository.deleteAllByMemberIdAndType(member.getId(), FcmMessageType.SCHOOL_NOTICE);

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
