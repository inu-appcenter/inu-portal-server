package kr.inuappcenterportal.inuportal.domain.reservation.service;

import kr.inuappcenterportal.inuportal.domain.reservation.dto.ReservationCreate;
import kr.inuappcenterportal.inuportal.domain.reservation.dto.ReservationDetail;
import kr.inuappcenterportal.inuportal.domain.reservation.dto.ReservationPreview;
import kr.inuappcenterportal.inuportal.domain.reservation.implement.ReservationCreator;
import kr.inuappcenterportal.inuportal.domain.reservation.implement.ReservationReader;
import kr.inuappcenterportal.inuportal.domain.reservation.implement.ReservationRemover;
import kr.inuappcenterportal.inuportal.domain.reservation.implement.ReservationVerifier;

import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationVerifier reservationVerifier;
    private final ReservationCreator reservationCreator;
    private final ReservationReader reservationReader;
    private final ReservationRemover reservationRemover;

    public Long create(ReservationCreate request, Long itemId ,Long memberId) {
        reservationVerifier.validateDates(request.getStartDateTime(), request.getEndDateTime());
        reservationVerifier.checkDuplicateReservation(memberId);
        return reservationCreator.create(request, itemId, memberId);
    }

    public ReservationDetail get(Long reservationId) {
        return reservationReader.get(reservationId);
    }

    public ListResponseDto<ReservationPreview> getListByItemId(Long itemId, int page) {
        return reservationReader.getListByItemId(itemId, page);
    }

    public Long deleteByOwner(Long itemId, Long memberId) {
        return reservationRemover.delete(itemId, memberId);
    }

    public ListResponseDto<ReservationPreview> getList(int page) {
        return reservationReader.getList(page);
    }


}
