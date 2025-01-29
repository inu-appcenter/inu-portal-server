package kr.inuappcenterportal.inuportal.domain.lostProperty.implement;

import kr.inuappcenterportal.inuportal.domain.book.dto.BookPreview;
import kr.inuappcenterportal.inuportal.domain.book.model.Book;
import kr.inuappcenterportal.inuportal.domain.lostProperty.dto.LostPropertyDetail;
import kr.inuappcenterportal.inuportal.domain.lostProperty.dto.LostPropertyPreview;
import kr.inuappcenterportal.inuportal.domain.lostProperty.dto.LostPropertyRegister;
import kr.inuappcenterportal.inuportal.domain.lostProperty.dto.LostPropertyUpdate;
import kr.inuappcenterportal.inuportal.domain.lostProperty.model.LostProperty;
import kr.inuappcenterportal.inuportal.domain.lostProperty.repository.LostPropertyRepository;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LostPropertyProcessor {

    private final LostPropertyRepository lostPropertyRepository;

    public Long register(LostPropertyRegister request, int imageCount) {
        LostProperty lostProperty = lostPropertyRepository.save(LostProperty.create(request.getName(), request.getContent(), imageCount));
        return lostProperty.getId();
    }

    @Transactional(readOnly = true)
    public ListResponseDto<LostPropertyPreview> getList(int page) {
        Page<LostProperty> lostProperties = lostPropertyRepository.findAll(PageRequest.of(--page, 8, Sort.by(Sort.Direction.DESC, "id")));
        List<LostPropertyPreview> previews = getPreviews(lostProperties);
        return getPreviewListResponseDto(lostProperties, previews);
    }

    @Transactional(readOnly = true)
    public LostPropertyDetail getDetail(Long lostPropertyId) {
        LostProperty lostProperty = lostPropertyRepository.findById(lostPropertyId).orElseThrow(() -> new MyException(MyErrorCode.LOST_PROPERTY_NOT_FOUND));
        return LostPropertyDetail.from(lostProperty);
    }

    public void delete(Long lostPropertyId) {
        lostPropertyRepository.deleteById(lostPropertyId);
    }

    @Transactional
    public void update(LostPropertyUpdate request, int imageCount, Long lostPropertyId) {
        LostProperty lostProperty = lostPropertyRepository.findById(lostPropertyId).orElseThrow(() -> new MyException(MyErrorCode.LOST_PROPERTY_NOT_FOUND));
        lostProperty.update(request.getName(), request.getContent(), imageCount);
    }

    private List<LostPropertyPreview> getPreviews(Page<LostProperty> lostProperties) {
        return lostProperties.stream()
                .map(lostProperty -> new LostPropertyPreview(lostProperty.getId(), lostProperty.getName(), lostProperty.getContent(),
                        lostProperty.getImageCount(), lostProperty.getCreateDate()))
                .toList();
    }

    private static ListResponseDto<LostPropertyPreview> getPreviewListResponseDto(Page<LostProperty> lostProperties, List<LostPropertyPreview> lostPropertyPreviews) {
        long total = lostProperties.getTotalElements();
        long pages = lostProperties.getTotalPages();
        return ListResponseDto.of(pages, total, lostPropertyPreviews);
    }
}
