package kr.inuappcenterportal.inuportal.domain.item.service;

import kr.inuappcenterportal.inuportal.domain.item.dto.ItemDetail;
import kr.inuappcenterportal.inuportal.domain.item.dto.ItemPreview;
import kr.inuappcenterportal.inuportal.domain.item.dto.ItemRegister;
import kr.inuappcenterportal.inuportal.domain.item.dto.ItemUpdate;
import kr.inuappcenterportal.inuportal.domain.item.implement.ItemProcessor;
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
public class ItemService {

    private final ItemProcessor itemProcessor;
    private final ImageService imageService;
    @Value("${itemImagePath}")
    private String itemImagePath;

    public Long register(ItemRegister request, List<MultipartFile> images) throws IOException {
        Long itemId = itemProcessor.register(request, images);
        imageService.saveImageWithThumbnail(itemId, images, itemImagePath);
        return itemId;
    }

    public ItemDetail get(Long itemId) {
        return itemProcessor.getDetail(itemId);
    }


    public void update(ItemUpdate itemUpdate, List<MultipartFile> images, Long itemId) throws IOException {
        itemProcessor.update(itemUpdate, images.size(), itemId);
        imageService.updateImages(itemId, images, itemImagePath);
    }

    public void delete(Long itemId) {
        itemProcessor.delete(itemId);
        imageService.deleteImages(itemId, itemImagePath);
    }

    public List<ItemPreview> getList() {
        return itemProcessor.getList();
    }



}
