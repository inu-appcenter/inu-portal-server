package kr.inuappcenterportal.inuportal.domain.reservation.implement;

import kr.inuappcenterportal.inuportal.domain.item.model.Item;
import kr.inuappcenterportal.inuportal.domain.item.repository.ItemRepository;
import kr.inuappcenterportal.inuportal.domain.reservation.model.Reservation;
import kr.inuappcenterportal.inuportal.domain.reservation.repository.ReservationRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class ReservationVerifier {

    private final ReservationRepository reservationRepository;
    private final ItemRepository itemRepository;

    public void validateDates(LocalDateTime rentalTime, LocalDateTime returnTime) {
        LocalDateTime reservationTime = LocalDateTime.now();
        LocalDateTime rentalMinTime = reservationTime.plusDays(3);
        LocalDateTime rentalMaxTime = reservationTime.plusDays(14);
        LocalTime openTime = LocalTime.of(10, 0);
        LocalTime closeTime = LocalTime.of(17, 0);

        if (rentalTime.isBefore(rentalMinTime) || rentalTime.isAfter(rentalMaxTime)) {
            throw new MyException(MyErrorCode.INAPPROPRIATE_RESERVATION_PERIOD);
        }
        if (rentalTime.getDayOfWeek() == DayOfWeek.SATURDAY || rentalTime.getDayOfWeek() == DayOfWeek.SUNDAY
                || returnTime.getDayOfWeek() == DayOfWeek.SATURDAY || returnTime.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new MyException(MyErrorCode.INAPPROPRIATE_RENTAL_DAY);
        }
        if (rentalTime.toLocalTime().isBefore(openTime) || rentalTime.toLocalTime().isAfter(closeTime)
            || returnTime.toLocalTime().isBefore(openTime) || returnTime.toLocalTime().isAfter(closeTime)) {
            throw new MyException(MyErrorCode.INAPPROPRIATE_RENTAL_TIME);
        }

    }

    public void checkDuplicateReservation(Long memberId) {
        if(reservationRepository.existsByMemberId(memberId)) throw new MyException(MyErrorCode.DUPLICATE_RESERVATION);
    }

    public void validQuantity(Long itemId, LocalDateTime rentalTime, LocalDateTime returnTime, int quantity){
        if(getRemainQuantity(itemId,rentalTime,returnTime) - quantity < 0){
            throw new MyException(MyErrorCode.OUT_OF_ITEM);
        }
    }

    public int getRemainQuantity(Long itemId, LocalDateTime rentalTime, LocalDateTime returnTime){
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new MyException(MyErrorCode.ITEM_NOT_FOUND));
        Long reservatedQuantity = reservationRepository.findTotalQuantityByItemIdBetween(itemId,rentalTime,returnTime);
        return (int) (item.getTotalQuantity() - reservatedQuantity);
    }

    public void validateConfirm(Reservation reservation){
        validQuantity(reservation.getItemId(), reservation.getStartDateTime(), reservation.getEndDateTime(), reservation.getQuantity());
    }
}
