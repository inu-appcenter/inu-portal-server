package kr.inuappcenterportal.inuportal.domain.keyword.service;

import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.keyword.domain.Keyword;
import kr.inuappcenterportal.inuportal.domain.keyword.dto.res.KeywordResponse;
import kr.inuappcenterportal.inuportal.domain.keyword.repository.KeywordRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
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
    public KeywordResponse addKeyword(Member member, String keywordString, Department department) {
        Keyword keyword = createKeywordEntity(member.getId(), keywordString, department);
        keywordRepository.save(keyword);

        return KeywordResponse.from(keyword);
    }

    @Transactional
    public void departmentNotifyMatchedUsers(DepartmentNotice departmentNotice, Department department) {
        List<Long> memberIds = keywordRepository
                .findMemberIdsByKeywordAndDepartmentMatches(departmentNotice.getTitle(), department);
        if (memberIds.isEmpty()) return;

        List<String> tokens = fcmTokenRepository.findFcmTokensByMemberIds(memberIds);

        fcmService.sendKeywordNotice(tokens, departmentNotice.getTitle(), "새로운 학과 공지사항이 등록되었습니다.");
    }

    private Keyword createKeywordEntity(Long memberId, String keywordString, Department department) {
        return Keyword.builder()
                .memberId(memberId)
                .keyword(keywordString)
                .department(department)
                .build();
    }
}
