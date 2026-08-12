package kr.inuappcenterportal.inuportal.domain.suggestion.service;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionAnswerRequest;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionListResponse;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionRequest;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionResponse;
import kr.inuappcenterportal.inuportal.domain.suggestion.enums.SuggestionCategory;
import kr.inuappcenterportal.inuportal.domain.suggestion.model.Suggestion;
import kr.inuappcenterportal.inuportal.domain.suggestion.repository.SuggestionRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SuggestionService {

    private final SuggestionRepository suggestionRepository;

    @Transactional
    public Long saveSuggestion(SuggestionRequest suggestionRequest, Member member) {
        Suggestion suggestion = Suggestion.builder()
                .content(suggestionRequest.getContent())
                .cheerMessage(suggestionRequest.getCheerMessage())
                .member(member)
                .category(SuggestionCategory.from(suggestionRequest.getCategory()))
                .appVersion(suggestionRequest.getAppVersion())
                .osType(suggestionRequest.getOsType())
                .osVersion(suggestionRequest.getOsVersion())
                .deviceModel(suggestionRequest.getDeviceModel())
                .build();
        return suggestionRepository.save(suggestion).getId();
    }

    public SuggestionResponse getSuggestion(Long suggestionId, Member member) {
        Suggestion suggestion = suggestionRepository.findByIdWithMember(suggestionId)
                .orElseThrow(() -> new MyException(MyErrorCode.SUGGESTION_NOT_FOUND));
        if (!suggestion.getMember().getId().equals(member.getId()) && !member.getRoles().contains("ROLE_ADMIN")) {
            throw new MyException(MyErrorCode.HAS_NOT_SUGGESTION_AUTHORIZATION);
        }
        return SuggestionResponse.of(suggestion);
    }

    public SuggestionListResponse getSuggestionList(int page, Member member) {
        Pageable pageable = PageRequest.of(page > 0 ? --page : page, 8);
        Page<Suggestion> suggestions = member.getRoles().contains("ROLE_ADMIN")
                ? suggestionRepository.findAllWithMember(pageable)
                : suggestionRepository.findAllByMemberAndIsDeletedFalse(member, pageable);
        return SuggestionListResponse.of(suggestions);
    }

    @Transactional
    public Long deleteSuggestion(Long suggestionId, Member member) {
        Suggestion suggestion = validHasAuthorizationSuggestion(suggestionId, member);
        suggestion.deleteSuggestion();
        return suggestionId;
    }

    @Transactional
    public Long answerSuggestion(Long suggestionId, SuggestionAnswerRequest suggestionAnswerRequest) {
        Suggestion suggestion = suggestionRepository.findByIdAndIsDeletedFalse(suggestionId)
                .orElseThrow(() -> new MyException(MyErrorCode.SUGGESTION_NOT_FOUND));
        suggestion.registerAnswer(suggestionAnswerRequest.getAnswerContent(), suggestionAnswerRequest.getStatus());
        return suggestionId;
    }

    private Suggestion validHasAuthorizationSuggestion(Long suggestionId, Member member) {
        Suggestion suggestion = suggestionRepository.findByIdAndIsDeletedFalse(suggestionId)
                .orElseThrow(() -> new MyException(MyErrorCode.SUGGESTION_NOT_FOUND));
        if (!suggestion.getMember().getId().equals(member.getId()) && !member.getRoles().contains("ROLE_ADMIN")) {
            throw new MyException(MyErrorCode.HAS_NOT_SUGGESTION_AUTHORIZATION);
        }
        return suggestion;
    }
}
