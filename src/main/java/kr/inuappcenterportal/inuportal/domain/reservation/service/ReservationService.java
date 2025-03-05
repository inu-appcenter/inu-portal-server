package kr.inuappcenterportal.inuportal.domain.reservation.service;

import kr.inuappcenterportal.inuportal.domain.reservation.dto.ReservationCreate;
import kr.inuappcenterportal.inuportal.domain.reservation.dto.ReservationDetail;
import kr.inuappcenterportal.inuportal.domain.reservation.dto.ReservationPreview;
import kr.inuappcenterportal.inuportal.domain.reservation.enums.ReservationStatus;
import kr.inuappcenterportal.inuportal.domain.reservation.implement.ReservationCreator;
import kr.inuappcenterportal.inuportal.domain.reservation.implement.ReservationReader;
import kr.inuappcenterportal.inuportal.domain.reservation.implement.ReservationRemover;
import kr.inuappcenterportal.inuportal.domain.reservation.implement.ReservationVerifier;

import kr.inuappcenterportal.inuportal.domain.reservation.model.Reservation;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationVerifier reservationVerifier;
    private final ReservationCreator reservationCreator;
    private final ReservationReader reservationReader;
    private final ReservationRemover reservationRemover;

    public Long create(ReservationCreate request, Long itemId ,Long memberId) {
        reservationVerifier.validateDates(request.getStartDateTime(), request.getEndDateTime());
        reservationVerifier.validQuantity(itemId, request.getStartDateTime(), request.getEndDateTime(), request.getQuantity());
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
        reservationRemover.delete(reservationId, memberId);
        return reservationId;
    }

    public Long deleteByAdmin(Long reservationId) {
        reservationRemover.deleteByAdmin(reservationId);
        return reservationId;
    }

    public ListResponseDto<ReservationPreview> getList(int page, Long memberId) {
        return reservationReader.getList(page, memberId);
    }

    @Transactional
    public Long confirmOrRejectReservation(Long reservationId, String status) {
        Reservation reservation = reservationReader.get(reservationId);
        if(status.equals(ReservationStatus.CONFIRM.name())){
            reservationVerifier.validateConfirm(reservation);
        }
        reservation.confirmOrReject(status);
        return reservationId;
    }

    public int checkAbleReserveQuantity(LocalDateTime startDateTime, LocalDateTime endDateTime ,Long itemId){
        reservationVerifier.validateDates(startDateTime, endDateTime);
        return reservationVerifier.getRemainQuantity(itemId, startDateTime, endDateTime);
    }
}
