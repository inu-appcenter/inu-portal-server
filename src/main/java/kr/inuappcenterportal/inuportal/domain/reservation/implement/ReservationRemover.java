package kr.inuappcenterportal.inuportal.domain.reservation.implement;

import kr.inuappcenterportal.inuportal.domain.reservation.model.Reservation;
import kr.inuappcenterportal.inuportal.domain.reservation.repository.ReservationRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ReservationRemover {


    private final ReservationRepository reservationRepository;

    @Transactional
    public Reservation delete(Long reservationId, Long memberId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(() -> new MyException(MyErrorCode.RESERVATION_NOT_FOUND));
        if (Objects.equals(reservation.getMemberId(), memberId)) reservationRepository.delete(reservation);
        else throw new MyException(MyErrorCode.HAS_NOT_RESERVATION_AUTHORIZATION);
        return reservation;
    }

    @Transactional
    public Reservation deleteByAdmin(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(() -> new MyException(MyErrorCode.RESERVATION_NOT_FOUND));
        reservationRepository.deleteById(reservationId);
        return reservation;
    }

    public List<Reservation> findExpiredReservations(LocalDateTime now) {
        return reservationRepository.findByEndDateTimeBefore(now);
    }
}
