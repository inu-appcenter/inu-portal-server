package kr.inuappcenterportal.inuportal.domain.reservation.service;

import kr.inuappcenterportal.inuportal.domain.item.implement.ItemProcessor;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationVerifier reservationVerifier;
    private final ReservationCreator reservationCreator;
    private final ReservationReader reservationReader;
    private final ReservationRemover reservationRemover;
    private final ItemProcessor itemProcessor;

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
        Reservation reservation = reservationRemover.delete(reservationId, memberId);
        itemProcessor.rollbackItemQuantity(reservation.getItemId());
        return reservationId;
    }

    public Long deleteByAdmin(Long reservationId) {
        Reservation reservation = reservationRemover.deleteByAdmin(reservationId);
        itemProcessor.rollbackItemQuantity(reservation.getItemId());
        return reservationId;
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

    @Scheduled(cron = "0 0 3 * * *")
    public void deleteExpiredReservations() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Reservation> expiredReservations = reservationRemover.findExpiredReservations(now);
            int deletedCount = 0;
            for (Reservation reservation : expiredReservations) {
                reservationRemover.deleteByAdmin(reservation.getId());
                itemProcessor.rollbackItemQuantity(reservation.getItemId());
                deletedCount++;
            }

            log.info(deletedCount + "개의 만료된 예약을 삭제하고, 아이템 수량을 롤백했습니다.");
        } catch (Exception e) {
            log.error("예약 삭제 중 오류 발생", e);
        }
    }


}
