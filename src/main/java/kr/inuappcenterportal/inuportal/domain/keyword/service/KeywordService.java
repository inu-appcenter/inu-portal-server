package kr.inuappcenterportal.inuportal.domain.keyword.service;

import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.keyword.domain.Keyword;
import kr.inuappcenterportal.inuportal.domain.keyword.dto.req.KeywordRequest;
import kr.inuappcenterportal.inuportal.domain.keyword.dto.res.KeywordResponse;
import kr.inuappcenterportal.inuportal.domain.keyword.enums.KeywordCategory;
import kr.inuappcenterportal.inuportal.domain.keyword.repository.KeywordRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.notice.model.DepartmentNotice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeywordService {

    private final KeywordRepository keywordRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final FcmService fcmService;

    @Transactional
    public KeywordResponse addKeyword(Member member, KeywordRequest request) {
        Keyword keyword = createKeywordEntity(member.getId(), request);
        keywordRepository.save(keyword);

        return KeywordResponse.from(keyword);
    }

    @Transactional
    public void departmentNotifyMatchedUsers(DepartmentNotice departmentNotice) {
        List<Long> memberIds = keywordRepository
                .findMemberIdsByKeywordAndKeywordCategoryMatches(departmentNotice.getTitle(), KeywordCategory.DEPARTMENT);
        if (memberIds.isEmpty()) return;

        List<String> tokens = fcmTokenRepository.findFcmTokensByMemberIds(memberIds);

        fcmService.sendKeywordNotice(tokens, departmentNotice.getTitle(), "새로운 학과 공지사항이 등록되었습니다.");
    }

    private Keyword createKeywordEntity(Long memberId, KeywordRequest request) {
        return Keyword.builder()
                .memberId(memberId)
                .keyword(request.keyword())
                .keywordCategory(request.keywordCategory())
                .build();
    }
}
