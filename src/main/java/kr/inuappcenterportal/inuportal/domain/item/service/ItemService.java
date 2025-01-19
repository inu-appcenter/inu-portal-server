package kr.inuappcenterportal.inuportal.domain.item.service;

import kr.inuappcenterportal.inuportal.domain.item.dto.ItemDetail;
import kr.inuappcenterportal.inuportal.domain.item.dto.ItemPreview;
import kr.inuappcenterportal.inuportal.domain.item.dto.ItemUpdate;
import kr.inuappcenterportal.inuportal.domain.item.enums.ItemCategory;
import kr.inuappcenterportal.inuportal.domain.item.model.Item;
import kr.inuappcenterportal.inuportal.domain.item.repository.ItemRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public Long register(Item item) {
        return itemRepository.save(item).getId();
    }

    public ItemDetail get(Long itemId) {
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new MyException(MyErrorCode.ITEM_NOT_FOUND));
        return ItemDetail.from(item);
    }

    @Transactional
    public void update(ItemUpdate itemUpdate, Long itemId) {
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new MyException(MyErrorCode.ITEM_NOT_FOUND));
        item.update(ItemCategory.from(itemUpdate.getItemCategory()), itemUpdate.getName(), itemUpdate.getTotalQuantity(), itemUpdate.getDeposit());
    }

    public void delete(Long itemId) {
        itemRepository.deleteById(itemId);
    }

    public List<ItemPreview> getList() {
        return itemRepository.findAll()
                .stream()
                .map(ItemPreview::from)
                .toList();
    }



}
