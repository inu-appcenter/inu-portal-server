package kr.inuappcenterportal.inuportal.domain.reservation.service;

import kr.inuappcenterportal.inuportal.domain.reservation.dto.ReservationCreate;
import kr.inuappcenterportal.inuportal.domain.reservation.dto.ReservationDetail;
import kr.inuappcenterportal.inuportal.domain.reservation.dto.ReservationPreview;
import kr.inuappcenterportal.inuportal.domain.reservation.implement.ReservationCreator;
import kr.inuappcenterportal.inuportal.domain.reservation.implement.ReservationReader;
import kr.inuappcenterportal.inuportal.domain.reservation.implement.ReservationRemover;
import kr.inuappcenterportal.inuportal.domain.reservation.implement.ReservationVerifier;

import kr.inuappcenterportal.inuportal.domain.reservation.model.Reservation;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ReservationDetail getDetail(Long reservationId) {
        return reservationReader.getDetail(reservationId);
    }

    public ListResponseDto<ReservationPreview> getListByItemId(Long itemId, int page) {
        return reservationReader.getListByItemId(itemId, page);
    }

    public Long deleteByOwner(Long reservationId, Long memberId) {
        return reservationRemover.delete(reservationId, memberId);
    }

    public Long deleteByAdmin(Long reservationId) {
        return reservationRemover.deleteByAdmin(reservationId);
    }

    public ListResponseDto<ReservationPreview> getList(int page, Long memberId) {
        return reservationReader.getList(page, memberId);
    }

    @Transactional
    public Long confirmOrRejectReservation(Long reservationId, String status) {
        Reservation reservation = reservationReader.get(reservationId);
        reservation.confirmOrReject(status);
        return reservationId;
    }


}
