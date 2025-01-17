package kr.inuappcenterportal.inuportal.domain.reservation.implement;

import kr.inuappcenterportal.inuportal.domain.reservation.model.Reservation;
import kr.inuappcenterportal.inuportal.domain.reservation.repository.ReservationRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ReservationRemover {


    private final ReservationRepository reservationRepository;

    @Transactional
    public Long delete(Long itemId, Long memberId) {
        Reservation reservation = reservationRepository.findByItemId(itemId);
        if (Objects.equals(reservation.getMemberId(), memberId)) reservationRepository.deleteByItemId(itemId);
        else throw new MyException(MyErrorCode.HAS_NOT_RESERVATION_AUTHORIZATION);
        return reservation.getId();
    }
}
