package kr.inuappcenterportal.inuportal.domain.item.implement;

import kr.inuappcenterportal.inuportal.domain.item.dto.ItemDetail;
import kr.inuappcenterportal.inuportal.domain.item.dto.ItemPreview;
import kr.inuappcenterportal.inuportal.domain.item.dto.ItemRegister;
import kr.inuappcenterportal.inuportal.domain.item.dto.ItemUpdate;
import kr.inuappcenterportal.inuportal.domain.item.enums.ItemCategory;
import kr.inuappcenterportal.inuportal.domain.item.model.Item;
import kr.inuappcenterportal.inuportal.domain.item.repository.ItemRepository;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ItemProcessor {

    private final ItemRepository itemRepository;

    public Long register(ItemRegister request, List<MultipartFile> images) {
        Item item = itemRepository.save(Item.create(request, images.size()));
        return item.getId();
    }

    @Transactional(readOnly = true)
    public ItemDetail getDetail(Long itemId) {
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new MyException(MyErrorCode.ITEM_NOT_FOUND));
        return ItemDetail.from(item);
    }

    @Transactional
    public void update(ItemUpdate itemUpdate, int imageCount, Long itemId) {
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new MyException(MyErrorCode.ITEM_NOT_FOUND));
        item.update(ItemCategory.from(itemUpdate.getItemCategory()), itemUpdate.getName(), itemUpdate.getTotalQuantity(), itemUpdate.getDeposit(), imageCount);
    }

    @Transactional
    public void delete(Long itemId) {
        itemRepository.deleteById(itemId);
    }

    @Transactional(readOnly = true)
    public List<ItemPreview> getList() {
        //Page<Item> items = itemRepository.findAll(PageRequest.of(--page, 8, Sort.by(Sort.Direction.DESC, "id")));
        List<Item> items = itemRepository.findAll();
        return getItemPreviews(items);
    }

    @Transactional
    public void rollbackItemQuantity(Long itemId) {
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new MyException(MyErrorCode.ITEM_NOT_FOUND));
        item.rollbackTotalQuantity();
    }

    private List<ItemPreview> getItemPreviews(List<Item> items) {
        return items.stream()
                .map(ItemPreview::from)
                .toList();
    }

    private ListResponseDto<ItemPreview> getItemPreviewListResponseDto(Page<Item> items, List<ItemPreview> itemPreviews) {
        long total = items.getTotalElements();
        int pages = items.getTotalPages();
        return ListResponseDto.of(pages, total, itemPreviews);
    }
}
