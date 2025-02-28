package kr.inuappcenterportal.inuportal.domain.reservation.implement;

import kr.inuappcenterportal.inuportal.domain.item.repository.ItemRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.reservation.dto.ReservationCreate;
import kr.inuappcenterportal.inuportal.domain.reservation.model.Reservation;
import kr.inuappcenterportal.inuportal.domain.reservation.repository.ReservationRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReservationCreator {

    private final ItemRepository itemRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public Long create(ReservationCreate request, Long itemId, Long memberId) {
        int updatedRows = itemRepository.decrease(itemId, request.getQuantity());
        if (updatedRows == 0) throw new MyException(MyErrorCode.OUT_OF_ITEM);
        Reservation reservation = Reservation.create(itemId, memberId, request.getStartDateTime(), request.getEndDateTime(), request.getPhoneNumber(), request.getQuantity());
        return reservationRepository.save(reservation).getId();
    }


}
