package kr.inuappcenterportal.inuportal.domain.keyword.service;

import io.jsonwebtoken.impl.crypto.MacProvider;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.keyword.domain.Keyword;
import kr.inuappcenterportal.inuportal.domain.keyword.dto.res.KeywordResponse;
import kr.inuappcenterportal.inuportal.domain.keyword.repository.KeywordRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import kr.inuappcenterportal.inuportal.domain.notice.model.DepartmentNotice;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeywordService {

    private final KeywordRepository keywordRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final FcmService fcmService;

    @Transactional(readOnly = true)
    public List<KeywordResponse> getKeywords(Member member) {
        List<Keyword> keywords = keywordRepository.findAllByMemberId(member.getId());
        return keywords.stream().map(KeywordResponse::from).toList();
    }

    @Transactional
    public KeywordResponse addKeyword(Member member, String keywordString, Department department) {
        Keyword keyword = createDepartmentKeyword(member.getId(), keywordString, department);
        keywordRepository.save(keyword);

        return KeywordResponse.from(keyword);
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

        List<String> tokens = new ArrayList<>(tokenAndMemberId.keySet());
        List<Long> safeMemberIds = new ArrayList<>(tokenAndMemberId.values());

        fcmService.sendKeywordNotice(safeMemberIds, tokens, "[" + department.getDepartmentName() + "] 키워드에 맞는 새 공지사항이 등록되었습니다.", departmentNotice.getTitle());
    }

    @Transactional
    public void departmentNotifyMatchedUsers(DepartmentNotice departmentNotice, Department department) {
        List<Long> memberIds = keywordRepository.findMemberIdsByDepartmentAndKeywordIsNull(department);
        if (memberIds.isEmpty()) return;

        List<FcmToken> fcmTokens = fcmTokenRepository.findFcmTokensByMemberIds(memberIds);

        Map<String, Long> tokenAndMemberId = fcmTokens.stream()
                .filter(t -> t.getMemberId() != null)
                .collect(Collectors.toMap(FcmToken::getToken, FcmToken::getMemberId));

        List<String> tokens = new ArrayList<>(tokenAndMemberId.keySet());
        List<Long> safeMemberIds = new ArrayList<>(tokenAndMemberId.values());

        fcmService.sendKeywordNotice(safeMemberIds, tokens, "[" + department.getDepartmentName() + "] 새로운 공지사항이 등록되었습니다.", departmentNotice.getTitle());
    }

    @Transactional
    public void deleteKeyword(Member member, Long keywordId) {
        Keyword keyword = getKeywordById(keywordId);
        validateKeywordOwnership(member.getId(), keywordId);
        keywordRepository.delete(keyword);
    }

    @Transactional(readOnly = true)
    public List<KeywordResponse> getDepartmentFcm(Member member) {
        List<Keyword> keywords = keywordRepository.findAllByMemberIdAndKeywordIsNull(member.getId());
        return keywords.stream().map(KeywordResponse::from).toList();
    }

    @Transactional
    public KeywordResponse addDepartmentFcm(Member member, Department department) {
        List<Keyword> toDeleteKeywords = keywordRepository.findAllByMemberId(member.getId());
        keywordRepository.deleteAll(toDeleteKeywords);

        Keyword keyword = createDepartmentKeyword(member.getId(), null, department);
        keywordRepository.save(keyword);

        return KeywordResponse.from(keyword);
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
}
