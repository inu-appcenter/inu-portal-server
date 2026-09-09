package kr.inuappcenterportal.inuportal.domain.suggestion.service;

import kr.inuappcenterportal.inuportal.domain.image.service.ImageService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionListResponse;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionRequest;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionResponse;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionStatusRequest;
import kr.inuappcenterportal.inuportal.domain.suggestion.enums.SuggestionCategory;
import kr.inuappcenterportal.inuportal.domain.suggestion.enums.SuggestionStatus;
import kr.inuappcenterportal.inuportal.domain.suggestion.model.Suggestion;
import kr.inuappcenterportal.inuportal.domain.suggestion.repository.SuggestionRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SuggestionService {

    private static final int MAX_IMAGE_COUNT = 5;

    private final SuggestionRepository suggestionRepository;
    private final ImageService imageService;

    @Value("${suggestionImagePath}")
    private String suggestionImagePath;

    @Transactional
    public Long saveSuggestion(SuggestionRequest suggestionRequest, Member member, List<MultipartFile> images) throws IOException {
        validateImages(images);
        Suggestion suggestion = Suggestion.create(
                suggestionRequest.getContent(),
                suggestionRequest.getCheerMessage(),
                member,
                SuggestionCategory.from(suggestionRequest.getCategory()),
                suggestionRequest.getAppVersion(),
                suggestionRequest.getOsType(),
                suggestionRequest.getOsVersion(),
                suggestionRequest.getDeviceModel()
        );
        suggestion = suggestionRepository.save(suggestion);
        if (images != null && !images.isEmpty()) {
            Files.createDirectories(Paths.get(suggestionImagePath));
            imageService.saveImage(suggestion.getId(), images, suggestionImagePath);
            suggestion.updateImageCount(images.size());
        }
        return suggestion.getId();
    }

    public SuggestionResponse getSuggestion(Long suggestionId, Member member) {
        Suggestion suggestion = suggestionRepository.findByIdWithMember(suggestionId)
                .orElseThrow(() -> new MyException(MyErrorCode.SUGGESTION_NOT_FOUND));
        if (!suggestion.getMember().getId().equals(member.getId()) && !member.getRoles().contains("ROLE_ADMIN")) {
            throw new MyException(MyErrorCode.HAS_NOT_SUGGESTION_AUTHORIZATION);
        }
        return SuggestionResponse.of(suggestion);
    }

    public byte[] getSuggestionImage(Long suggestionId, Long imageId, Member member) {
        Suggestion suggestion = suggestionRepository.findByIdWithMember(suggestionId)
                .orElseThrow(() -> new MyException(MyErrorCode.SUGGESTION_NOT_FOUND));
        if (!suggestion.getMember().getId().equals(member.getId()) && !member.getRoles().contains("ROLE_ADMIN")) {
            throw new MyException(MyErrorCode.HAS_NOT_SUGGESTION_AUTHORIZATION);
        }
        return imageService.getImage(suggestionId, imageId, suggestionImagePath);
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
    public Long changeSuggestionStatus(Long suggestionId, SuggestionStatusRequest suggestionStatusRequest) {
        Suggestion suggestion = suggestionRepository.findByIdAndIsDeletedFalse(suggestionId)
                .orElseThrow(() -> new MyException(MyErrorCode.SUGGESTION_NOT_FOUND));
        suggestion.changeStatus(SuggestionStatus.from(suggestionStatusRequest.getStatus()));
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

    private void validateImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        if (images.size() > MAX_IMAGE_COUNT) {
            throw new MyException(MyErrorCode.SUGGESTION_IMAGE_LIMIT_EXCEEDED);
        }
        for (MultipartFile image : images) {
            String contentType = image.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new MyException(MyErrorCode.INVALID_IMAGE_TYPE);
            }
        }
    }
}
