package kr.inuappcenterportal.inuportal.domain.reservation.dto;

import kr.inuappcenterportal.inuportal.domain.reservation.enums.ReservationStatus;
import kr.inuappcenterportal.inuportal.domain.reservation.model.Reservation;
import lombok.Builder;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReservationDetail {

    private Long itemId;
    private Long memberId;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDateTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDateTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    private ReservationStatus reservationStatus;
    private String studentId;
    private String phoneNumber;

    public static ReservationDetail from(Reservation reservation, String studentId) {
        return ReservationDetail.builder()
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
