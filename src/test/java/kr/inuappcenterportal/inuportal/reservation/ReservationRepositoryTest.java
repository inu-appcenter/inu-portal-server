package kr.inuappcenterportal.inuportal.reservation;

import jakarta.transaction.Transactional;
import kr.inuappcenterportal.inuportal.domain.item.model.Item;
import kr.inuappcenterportal.inuportal.domain.item.repository.ItemRepository;
import kr.inuappcenterportal.inuportal.domain.reservation.enums.ReservationStatus;
import kr.inuappcenterportal.inuportal.domain.reservation.model.Reservation;
import kr.inuappcenterportal.inuportal.domain.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static java.time.LocalDateTime.now;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
public class ReservationRepositoryTest {
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private ItemRepository itemRepository;



    @Test
    @DisplayName("일정 시간 안에 있는 예약된 물품의 갯수를 반환합니다.")
    public void checkRemainQuantityTest(){
        Item item = Item.builder().totalQuantity(10).build();
        Long itemId = itemRepository.save(item).getId();
        Reservation res1 = Reservation.builder().itemId(itemId).quantity(1).startDateTime(now().plusHours(1)).endDateTime(now().plusHours(2)).reservationStatus(ReservationStatus.CONFIRM).build();
        Reservation res2 = Reservation.builder().itemId(itemId).quantity(2).startDateTime(now().plusHours(3)).endDateTime(now().plusHours(4)).reservationStatus(ReservationStatus.CONFIRM).build();
        Reservation res3 = Reservation.builder().itemId(itemId).quantity(3).startDateTime(now().plusHours(5)).endDateTime(now().plusHours(6)).reservationStatus(ReservationStatus.CONFIRM).build();
        Reservation res4 = Reservation.builder().itemId(itemId).quantity(4).startDateTime(now().plusHours(10)).endDateTime(now().plusHours(15)).reservationStatus(ReservationStatus.CONFIRM).build();
        Reservation res5 = Reservation.builder().itemId(itemId).quantity(5).startDateTime(now().plusHours(1)).endDateTime(now().plusHours(2)).reservationStatus(ReservationStatus.PENDING).build();
        reservationRepository.saveAll(List.of(res1,res2,res3,res4,res5));

        Long quantity = reservationRepository.findTotalQuantityByItemIdBetween(itemId,now().minusHours(1),now().plusHours(8));
        assertEquals(quantity,6);
    }
}
