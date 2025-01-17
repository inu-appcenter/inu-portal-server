package kr.inuappcenterportal.inuportal.domain.item.repository;

import kr.inuappcenterportal.inuportal.domain.item.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {

    @Modifying
    @Query("UPDATE Item i SET i.totalQuantity = i.totalQuantity - 1 WHERE i.id = :itemId AND i.totalQuantity > 0")
    int decrease(@Param("itemId") Long itemId);
}
