package kr.inuappcenterportal.inuportal.domain.reservation.dto;

import kr.inuappcenterportal.inuportal.domain.reservation.enums.ReservationStatus;
import kr.inuappcenterportal.inuportal.domain.reservation.model.Reservation;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReservationPreview {

    private Long itemId;

    private Long memberId;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private LocalDateTime createdAt;
    private ReservationStatus reservationStatus;

    public static ReservationPreview from(Reservation reservation) {
        return ReservationPreview.builder()
                .itemId(reservation.getItemId())
                .memberId(reservation.getMemberId())
                .startDateTime(reservation.getStartDateTime())
                .endDateTime(reservation.getEndDateTime())
                .createdAt(reservation.getCreatedAt())
                .reservationStatus(reservation.getReservationStatus())
                .build();
    }

}
