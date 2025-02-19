package kr.inuappcenterportal.inuportal.domain.reservation.dto;

import kr.inuappcenterportal.inuportal.domain.reservation.enums.ReservationStatus;
import kr.inuappcenterportal.inuportal.domain.reservation.model.Reservation;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReservationPreview {

    private Long reservationId;

    private Long itemId;

    private Long memberId;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private LocalDateTime createdAt;
    private ReservationStatus reservationStatus;
    private String studentId;
    private String phoneNumber;

    public static ReservationPreview from(Reservation reservation, String studentId) {
        return ReservationPreview.builder()
                .reservationId(reservation.getId())
                .itemId(reservation.getItemId())
                .memberId(reservation.getMemberId())
                .startDateTime(reservation.getStartDateTime())
                .endDateTime(reservation.getEndDateTime())
                .createdAt(reservation.getCreatedAt())
                .reservationStatus(reservation.getReservationStatus())
                .studentId(studentId)
                .phoneNumber(reservation.getPhoneNumber())
                .build();
    }

}
