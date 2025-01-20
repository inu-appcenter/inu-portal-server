package kr.inuappcenterportal.inuportal.domain.lostProperty.service;

import kr.inuappcenterportal.inuportal.domain.lostProperty.dto.LostPropertyDetail;
import kr.inuappcenterportal.inuportal.domain.lostProperty.dto.LostPropertyPreview;
import kr.inuappcenterportal.inuportal.domain.lostProperty.dto.LostPropertyRegister;
import kr.inuappcenterportal.inuportal.domain.lostProperty.dto.LostPropertyUpdate;
import kr.inuappcenterportal.inuportal.domain.lostProperty.implement.LostPropertyProcessor;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LostPropertyService {

    private final LostPropertyProcessor lostPropertyProcessor;
    private final ImageService imageService;
    @Value("${lostPropertyImagePath}")
    private String lostPropertyImagePath;

    public Long register(LostPropertyRegister request, List<MultipartFile> images) throws IOException {
        Long lostPropertyId = lostPropertyProcessor.register(request, images.size());
        imageService.saveImageWithThumbnail(lostPropertyId, images, lostPropertyImagePath);
        return lostPropertyId;
    }

    public ListResponseDto<LostPropertyPreview> getList(int page) {
        return lostPropertyProcessor.getList(page);
    }

    public LostPropertyDetail get(Long lostPropertyId) {
        return lostPropertyProcessor.getDetail(lostPropertyId);
    }

    public void delete(Long lostPropertyId) {
        lostPropertyProcessor.delete(lostPropertyId);
        imageService.deleteImages(lostPropertyId, lostPropertyImagePath);
    }

    public void update(LostPropertyUpdate request, List<MultipartFile> images, Long lostPropertyId) throws IOException {
        lostPropertyProcessor.update(request, images.size(), lostPropertyId);
        imageService.updateImages(lostPropertyId, images, lostPropertyImagePath);
    }

}
